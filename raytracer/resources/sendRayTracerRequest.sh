# Add scene.txt raw content to JSON.
cat test01.txt | jq -sR '{scene: .}' > payload.json

# Add texmap.bmp binary to JSON (optional step, required only for some scenes).
#hexdump -ve '1/1 "%u\n"' 01.bmp | jq -s --argjson original "$(<payload.json)" '$original * {texmap: .}' > payload.json

# Send the request.
curl -X POST http://127.0.0.1:8000/raytracer?scols=400\&srows=300\&wcols=400\&wrows=300\&coff=0\&roff=0\&aa=false --data @"payload.json" > result.txt

# Remove a formatting string (remove everything before the comma).
sed -i 's/^[^{]*,//' result.txt

# Decode from Base64.
base64 -d result.txt > result.bmp