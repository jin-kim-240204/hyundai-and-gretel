"""Exercise the app's Room 1→2→3 and 2→3 migration SQL on host SQLite."""
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


def check_migration(source_version, target_version):
    db = sqlite3.connect(":memory:")
    fresh = sqlite3.connect(":memory:")
    db.execute("PRAGMA foreign_keys = ON")
    create_schema(db, source_version)
    create_schema(fresh, target_version)
    db.execute(
        "INSERT INTO road_notes (noteId, authorId, content, category, latitude, longitude, "
        "anchorOffsetMillis, createdAtUtcMillis, expiresAtUtcMillis, h3CellId, routeId, direction, progressMeters, scenarioId) "
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        ("note", "A", "  공사 아님. 물체가 있는 듯 🚧  ", "WARNING", 37.5, 127.03,
         30000, 1000, 601000, "h3", "route", 1, 10.0, "scenario"),
    )
    db.executemany(
        "INSERT INTO note_receipts VALUES (?, ?, ?, ?, ?)",
        [("note", "B", 1100, 32.0, None), ("note", "C", 1200, 12.5, 1300)],
    )
    db.execute("INSERT INTO demo_state VALUES (1, 'scenario')")
    tables = ("road_notes", "note_receipts", "demo_state")

    if source_version == 2:
        db.execute("UPDATE road_notes SET receiverGuidance = '수신 안내문' WHERE noteId = 'note'")
    db.execute(
        "INSERT INTO road_notes (noteId, authorId, content, category, latitude, longitude, "
        "anchorOffsetMillis, createdAtUtcMillis, expiresAtUtcMillis) "
        "VALUES ('legacy', 'A', '이전 제보', 'INFORMATION', 37.501, 127.03, 0, 1000, 601000)"
    )
    db.row_factory = sqlite3.Row

    def rows(table):
        return [dict(row) for row in db.execute(f"SELECT * FROM {table} ORDER BY rowid")]

    original = {table: rows(table) for table in tables}
    migration = (STORAGE / "NoteDatabase.kt").read_text()
    registered, = re.findall(r'\.addMigrations\(([^)]+)\)', migration)
    db.commit()
    with db:
        db.execute("BEGIN")
        for version in range(source_version, target_version):
            name = f"MIGRATION_{version}_{version + 1}"
            assert name in registered.split(", ")
            block, = re.findall(rf"val {name} = object : Migration\({version}, {version + 1}\) \{{(.*?)\n}}",
                                migration, re.S)
            statements = re.findall(r'db\.execSQL\("([^"]+)"\)', block)
            assert statements
            for sql in statements:
                db.execute(sql)
    for note in original["road_notes"]:
        note.setdefault("receiverGuidance", None)
        if target_version == 3:
            del note["direction"]

    db.row_factory = None
    for table in tables:
        assert table_metadata(db, table) == table_metadata(fresh, table), table
    db.row_factory = sqlite3.Row
    for table in tables:
        assert rows(table) == original[table], table
    assert db.execute("PRAGMA foreign_key_check").fetchall() == []

    receipt_store = (STORAGE / "RoomReceiptStore.kt").read_text()
    inbox_sql, = re.findall(r'@Query\("(SELECT road_notes\.\*[^"]+)"\)', receipt_store)
    received, = db.execute(inbox_sql, {"recipient": "B"}).fetchall()
    for column, expected in original["road_notes"][0].items():
        assert received[column] == expected, column
    assert received["displayedAtUtcMillis"] is None
    acknowledged, = db.execute(inbox_sql, {"recipient": "C"}).fetchall()
    assert acknowledged["displayedAtUtcMillis"] == 1300

    # The same scenario-scoped SQL Room uses must include both indexed and unindexed rows.
    scoped_sql, = re.findall(r'@Query\("(SELECT .*scenarioId = :scenarioId[^"]+)"\)', migration)
    scoped = db.execute(scoped_sql, {"scenarioId": "scenario", "cells": "h3"}).fetchall()
    assert [row["noteId"] for row in scoped] == ["note"]
    db.execute("UPDATE road_notes SET scenarioId = 'scenario' WHERE noteId = 'legacy'")
    scoped = db.execute(scoped_sql, {"scenarioId": "scenario", "cells": "h3"}).fetchall()
    assert {row["noteId"] for row in scoped} == {"note", "legacy"}

    db.execute("DELETE FROM road_notes WHERE noteId = 'note'")
    assert rows("note_receipts") == []
    db.close()
    fresh.close()
    print(f"PASS: Room {source_version}→{target_version} SQL, row and schema parity, inbox, candidate scope, FK cascade")


if __name__ == "__main__":
    for source, target in ((1, 2), (1, 3), (2, 3)):
        check_migration(source, target)
