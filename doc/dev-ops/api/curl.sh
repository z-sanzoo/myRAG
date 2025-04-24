#curl http://127.0.0.1:11434/api/generate \
#  -H "Content-Type: application/json" \
#  -d '{
#        "model": "deepseek-r1:1.5b",
#        "prompt": "1+1",
#        "stream": false
#      }'


curl http://127.0.0.1:8090/api/v1/ollama/generate?model=deepseek-r1:1.5b&message=hello
