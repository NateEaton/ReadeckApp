#!/usr/bin/env python3
"""
Basic Kotlin syntax checker for catching common errors without a full Gradle build.

This script checks for:
- Balanced braces, brackets, and parentheses
- Properly matched delimiters

Usage:
    python3 scripts/check-kotlin-syntax.py                    # Check all Kotlin files
    python3 scripts/check-kotlin-syntax.py path/to/file.kt   # Check specific file
"""
import sys
from pathlib import Path

def check_kotlin_file(filepath):
    """Check a Kotlin file for common syntax errors."""
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    errors = []

    # Remove strings and comments for bracket matching
    def clean_code(text):
        in_string = False
        in_char = False
        in_comment = False
        in_block_comment = False
        cleaned = []
        i = 0
        while i < len(text):
            if in_block_comment:
                if i < len(text) - 1 and text[i:i+2] == '*/':
                    in_block_comment = False
                    i += 2
                    continue
                if text[i] == '\n':
                    cleaned.append('\n')
                i += 1
                continue
            if in_comment:
                if text[i] == '\n':
                    in_comment = False
                    cleaned.append('\n')
                i += 1
                continue
            if in_string:
                if text[i] == '"' and (i == 0 or text[i-1] != '\\'):
                    in_string = False
                i += 1
                continue
            if in_char:
                if text[i] == "'" and (i == 0 or text[i-1] != '\\'):
                    in_char = False
                i += 1
                continue

            if i < len(text) - 1 and text[i:i+2] == '//':
                in_comment = True
                i += 2
                continue
            if i < len(text) - 1 and text[i:i+2] == '/*':
                in_block_comment = True
                i += 2
                continue
            if text[i] == '"':
                in_string = True
                i += 1
                continue
            if text[i] == "'":
                in_char = True
                i += 1
                continue

            cleaned.append(text[i])
            i += 1

        return ''.join(cleaned)

    cleaned_content = clean_code(content)

    # Check balanced delimiters
    stack = []
    line_num = 1
    col_num = 0

    matching = {'}': '{', ']': '[', ')': '('}

    for char in cleaned_content:
        col_num += 1
        if char == '\n':
            line_num += 1
            col_num = 0
            continue

        if char in '{[(':
            stack.append((char, line_num, col_num))
        elif char in '}])':
            if not stack:
                errors.append(f"Line {line_num}, Col {col_num}: Unmatched closing '{char}'")
                continue
            opening = stack[-1][0]
            expected = matching[char]
            if opening != expected:
                errors.append(f"Line {line_num}, Col {col_num}: Mismatched delimiter. Found '{char}' but expected to close '{opening}' from line {stack[-1][1]}")
            stack.pop()

    for opening, line, col in stack:
        errors.append(f"Line {line}, Col {col}: Unclosed '{opening}'")

    return errors

def check_project_kotlin_files(project_dir):
    """Check all Kotlin files in the project."""
    project_path = Path(project_dir)
    kotlin_files = list(project_path.rglob('*.kt'))

    all_errors = {}
    file_count = 0

    for kt_file in kotlin_files:
        # Skip build directories
        if '/build/' in str(kt_file):
            continue

        file_count += 1
        errors = check_kotlin_file(kt_file)
        if errors:
            all_errors[str(kt_file)] = errors

    return all_errors, file_count

if __name__ == '__main__':
    project_root = Path(__file__).parent.parent

    if len(sys.argv) > 1:
        path = Path(sys.argv[1])
        if not path.is_absolute():
            path = project_root / path
    else:
        path = project_root

    if path.is_file():
        errors = check_kotlin_file(path)
        if errors:
            print(f"\n{path}:")
            for error in errors:
                print(f"  {error}")
            sys.exit(1)
        else:
            print(f"✓ {path}: No syntax errors detected")
    else:
        all_errors, file_count = check_project_kotlin_files(path)
        if all_errors:
            print("\n❌ Syntax errors found:\n")
            for file, errors in all_errors.items():
                print(f"{file}:")
                for error in errors:
                    print(f"  {error}")
            print(f"\nChecked {file_count} Kotlin files, {len(all_errors)} with errors")
            sys.exit(1)
        else:
            print(f"✓ All {file_count} Kotlin files passed basic syntax checks")
