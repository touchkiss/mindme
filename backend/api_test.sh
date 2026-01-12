invoke_url='https://integrate.api.nvidia.com/v1/chat/completions'

authorization_header='Authorization: Bearer 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'
accept_header='Accept: application/json'
content_type_header='Content-Type: application/json'

data=$'{
  "model": "minimaxai/minimax-m2",
  "messages": [
    {
      "role": "user",
      "content": ""
    }
  ],
  "temperature": 1,
  "top_p": 0.95,
  "frequency_penalty": 0,
  "presence_penalty": 0,
  "max_tokens": 8192,
  "stream": true
}'

response=$(curl --silent -i -w "\n%{http_code}" --request POST \
  --url "$invoke_url" \
  --header "$authorization_header" \
  --header "$accept_header" \
  --header "$content_type_header" \
  --data "$data"
)

echo "$response"