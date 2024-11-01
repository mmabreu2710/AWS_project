base64 imageproc/resources/airplane.jpg > temp.txt
echo -e "data:image/jpg;base64,$(cat temp.txt)" > temp.txt
curl -X POST http://127.0.0.1:8000/enhanceimage --data @"./temp.txt" > result.txt
sed -i 's/^[^,]*,//' result.txt
base64 -d result.txt > result.jpg