#!/bin/bash
#
# Цей скрипт довантажує важкі файли (whisper-моделі та нативні DLL),
# які НЕ зберігаються в git, а лежать окремо в GitHub Releases.
#
# Запуск:
#   bash scripts/download-assets.sh
#
# Запуск з вибором варіанту нативних бібліотек (за замовчуванням cuda):
#   VARIANT=vulkan bash scripts/download-assets.sh
#

# "set -e" означає: якщо будь-яка команда впаде з помилкою -
# одразу зупинити виконання скрипта, а не продовжувати далі "наосліп".
set -e

# Назва репозиторію на GitHub у форматі "власник/назва".
REPO="q1wekl/SFX-RTX-TripleX"

# Який варіант нативних бібліотек качати: "cuda" (NVIDIA) чи "vulkan".
# ":-cuda" означає "якщо змінна VARIANT не задана ззовні - взяти cuda за замовчуванням".
VARIANT="${VARIANT:-cuda}"

echo "=== Крок 1: Завантаження whisper-моделей (.bin) ==="

# Папка, куди підуть моделі. "-p" у mkdir означає "створити, включно з
# батьківськими папками, якщо їх ще немає; якщо вже є - не видавати помилку".
MODELS_DIR="Dyplom/models/whisper"
mkdir -p "$MODELS_DIR"

# Список файлів моделей, які треба завантажити.
# Це bash-масив: значення перелічені через пробіл у дужках.
MODEL_FILES=(
  "ggml-tiny.bin"
  "ggml-base.bin"
  "ggml-small.bin"
  "ggml-medium.bin"
  "ggml-large.bin"
  "ggml-large-v3.bin"
)

# Цикл "for FILE in список" - перебирає кожен елемент масиву по черзі
# і підставляє його у змінну $FILE.
for FILE in "${MODEL_FILES[@]}"; do
  echo "Завантажую модель: $FILE"

  # curl -L  -> йти за редіректами (GitHub Releases завжди редіректить
  #             на реальне сховище файлів, без -L файл не скачається)
  # -o шлях  -> куди зберегти завантажений файл
  # останній аргумент - прямий URL на файл у Release з тегом "models"
  curl -L -o "$MODELS_DIR/$FILE" \
    "https://github.com/$REPO/releases/download/models/$FILE"
done

echo "=== Крок 2: Завантаження нативних бібліотек ($VARIANT) ==="

# Назва архіву залежить від обраного варіанту: native-cuda.zip або native-vulkan.zip
ARCHIVE_NAME="native-$VARIANT.zip"
NATIVE_DIR="Dyplom/native/$VARIANT"

mkdir -p "$NATIVE_DIR"

echo "Завантажую архів: $ARCHIVE_NAME"
curl -L -o "$ARCHIVE_NAME" \
  "https://github.com/$REPO/releases/download/native/$ARCHIVE_NAME"

echo "Розпаковую $ARCHIVE_NAME у $NATIVE_DIR"
# unzip -o означає "перезаписати файли, якщо вони вже існують" (overwrite),
# -d вказує папку призначення для розпакування.
unzip -o "$ARCHIVE_NAME" -d "$NATIVE_DIR"

# Видаляємо сам архів після розпакування - він більше не потрібен,
# а лише займає місце на диску.
rm "$ARCHIVE_NAME"

echo "=== Готово! ==="
echo "Моделі: $MODELS_DIR"
echo "Нативні бібліотеки ($VARIANT): $NATIVE_DIR"
