"""
Script to package the complete application (Android frontend, OMR assets, Python Rasch backend, Supabase schema) into a clean, complete ZIP file.
"""

import os
import zipfile

PROJECT_ROOT = os.path.abspath(os.path.dirname(__file__))
ZIP_OUTPUTS = [
    os.path.join(PROJECT_ROOT, "omr_rasch_system.zip"),
    os.path.join(PROJECT_ROOT, "public", "omr_rasch_system.zip")
]

# Paths and extensions to exclude (build caches, binaries, temporary files)
EXCLUDE_DIRS = {
    ".gradle", "build", ".build-outputs", ".kotlin", "__pycache__", ".git", ".idea"
}
EXCLUDE_EXTENSIONS = {
    ".class", ".pyc", ".pyo", ".hprof"
}

def create_zip():
    os.makedirs(os.path.join(PROJECT_ROOT, "public"), exist_ok=True)
    target_zip = ZIP_OUTPUTS[0]
    print(f"Creating complete project archive: {target_zip}")

    total_files = 0
    total_uncompressed_bytes = 0

    with zipfile.ZipFile(target_zip, "w", zipfile.ZIP_DEFLATED) as zipf:
        for root, dirs, files in os.walk(PROJECT_ROOT):
            # Modify dirs in-place to avoid descending into excluded directories
            dirs[:] = [d for d in dirs if d not in EXCLUDE_DIRS and not d.startswith(".")]

            for file in files:
                # Do not zip the target zip itself or excluded extensions
                if file.endswith(".zip") or any(file.endswith(ext) for ext in EXCLUDE_EXTENSIONS):
                    continue

                full_path = os.path.join(root, file)
                rel_path = os.path.relpath(full_path, PROJECT_ROOT)

                # Skip files inside excluded dirs if any
                parts = set(rel_path.split(os.sep))
                if parts.intersection(EXCLUDE_DIRS):
                    continue

                zipf.write(full_path, rel_path)
                total_files += 1
                total_uncompressed_bytes += os.path.getsize(full_path)

    # Copy to public folder as well
    import shutil
    shutil.copyfile(target_zip, ZIP_OUTPUTS[1])

    zip_size_kb = os.path.getsize(target_zip) / 1024
    print(f"Archive successfully created!")
    print(f"- Total files packaged: {total_files}")
    print(f"- Uncompressed size: {total_uncompressed_bytes / (1024 * 1024):.2f} MB")
    print(f"- Compressed ZIP size: {zip_size_kb:.2f} KB")
    print(f"- Saved locations:")
    for out in ZIP_OUTPUTS:
        print(f"  * {out}")

if __name__ == "__main__":
    create_zip()
