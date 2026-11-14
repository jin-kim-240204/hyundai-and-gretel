"""Exercise the app's Room migration SQL on host SQLite with a v1 fixture."""
import json
import re
import sqlite3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SCHEMAS = ROOT / "app/schemas/com.jinkim.hyundaiAndGretel.notes.storage.NoteDatabase"
STORAGE = ROOT / "app/src/main/java/com/jinkim/hyundaiAndGretel/notes/storage"


def create_schema(db, version):
    schema = json.loads((SCHEMAS / f"{version}.json").read_text())["database"]
    for entity in schema["entities"]:
        table = entity["tableName"]
        db.execute(entity["createSql"].replace("${TABLE_NAME}", table))
        for index in entity.get("indices", []):
            db.execute(index["createSql"].replace("${TABLE_NAME}", table))


def table_metadata(db, table):
    return (
        db.execute(f"PRAGMA table_info({table})").fetchall(),
        db.execute(f"PRAGMA foreign_key_list({table})").fetchall(),
        db.execute(f"PRAGMA index_list({table})").fetchall(),
        db.execute(
            "SELECT sql FROM sqlite_master WHERE type = 'index' AND tbl_name = ? ORDER BY name",
            (table,),
        ).fetchall(),
    )


def main():
    db = sqlite3.connect(":memory:")
    fresh = sqlite3.connect(":memory:")
    db.execute("PRAGMA foreign_keys = ON")
    create_schema(db, 1)
    create_schema(fresh, 2)
    db.execute(
        "INSERT INTO road_notes VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        ("note", "A", "  공사 아님. 물체가 있는 듯 🚧  ", "WARNING", 37.5, 127.03,
         30000, 1000, 601000, "h3", "route", 1, 10.0, "scenario"),
    )
    db.executemany(
        "INSERT INTO note_receipts VALUES (?, ?, ?, ?, ?)",
        [("note", "B", 1100, 32.0, None), ("note", "C", 1200, 12.5, 1300)],
    )
    db.execute("INSERT INTO demo_state VALUES (1, 'scenario')")
    tables = ("road_notes", "note_receipts", "demo_state")
    original = {table: db.execute(f"SELECT * FROM {table}").fetchall() for table in tables}

    migration = (STORAGE / "NoteDatabase.kt").read_text()
    assert ".addMigrations(MIGRATION_1_2)" in migration
    sql, = re.findall(r'db\.execSQL\("([^"]+)"\)', migration)
    db.execute(sql)

    for table in tables:
        rows = db.execute(f"SELECT * FROM {table}").fetchall()
        expected = [row + (None,) for row in original[table]] if table == "road_notes" else original[table]
        assert rows == expected, table
        assert table_metadata(db, table) == table_metadata(fresh, table), table
    assert db.execute("PRAGMA foreign_key_check").fetchall() == []

    receipt_store = (STORAGE / "RoomReceiptStore.kt").read_text()
    inbox_sql, = re.findall(r'@Query\("(SELECT road_notes\.\*[^"]+)"\)', receipt_store)
    cursor = db.execute(inbox_sql, {"recipient": "B"})
    received, = cursor.fetchall()
    assert received[:len(original["road_notes"][0])] == original["road_notes"][0]
    guidance_index = [column[0] for column in cursor.description].index("receiverGuidance")
    assert received[guidance_index] is None

    guidance = "공사 중은 아니며 도로에 물체가 있는 것 같습니다."
    db.execute("INSERT INTO road_notes VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
               ("new", *original["road_notes"][0][1:], guidance))
    db.execute("INSERT INTO note_receipts VALUES ('new', 'B', 1400, 23.0, NULL)")
    assert db.execute(inbox_sql, {"recipient": "B"}).fetchall()[1][guidance_index] == guidance
    print("PASS: Room 1→2 SQL, schema parity, notes/receipts/scenario preservation, inbox guidance")


if __name__ == "__main__":
    main()
