@echo off
setlocal

:: [CLINICAL MAINTENANCE] LABORATORY MANAGEMENT SYSTEM - CATALOGUE CLEANUP
:: This tool wipes the clinical test catalogue to prevent redundancy.
:: When the application is restarted, it will re-seed correctly.

echo ====================================================
echo  [INFRASTRUCTURE] REPOSITORY INITIALIZATION
echo ====================================================
echo WARNING: This will permanently wipe existing clinical protocols.
echo Patient details and medical records will be preserved.
echo.

:: Execute via Python one-liner to avoid creating extra files
python -c "import sqlite3, os; db=os.path.join(os.path.expanduser('~'), '.lablms', 'laboratory.db'); conn=sqlite3.connect(db); c=conn.cursor(); c.execute('DELETE FROM test_parameters'); c.execute('DELETE FROM tests'); c.execute('DELETE FROM sqlite_sequence WHERE name IN (''tests'', ''test_parameters'')'); conn.commit(); conn.close(); print('[SUCCESS] Clinical catalogue wiped clean.')"

if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Synchronization failure. Ensure the LMS is closed and Python is installed.
) else (
    echo [STATUS] Ready for Global Master Sync.
)

echo ====================================================
echo Please run 'run.bat' now to complete the initialization.
pause
