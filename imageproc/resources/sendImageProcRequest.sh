# Encode in Base64.
base64 airplane.jpg > temp.txt                                            

# Append a formatting string.
echo -e "data:image/jpg;base64,$(cat temp.txt)" > temp.txt               

# Send the request.
curl -X POST http://127.0.0.1:8000/blurimage --data @"./temp.txt" > result.txt   

# Remove a formatting string (remove everything before the comma).
sed -i 's/^[^,]*,//' result.txt                                          

# Decode from Base64.
base64 -d result.txt > result.jpg  