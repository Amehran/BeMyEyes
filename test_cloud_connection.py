
import urllib.request
import json
import base64

# Cloud Run URL
url = "https://bemyeyes-backend-xz4vizivoq-uc.a.run.app/api/v1/analyze"

# 1x1 Black Pixel PNG (Base64)
# This mimics what the Android app sends
dummy_image_base64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAAAAAA6fptVAAAACklEQVR4nGP6DwABBAEbzKQ6AAAAAElFTkSuQmCC"

# Payload matching AnalysisRequest schema
payload = {
    "image_base64": dummy_image_base64,
    "user_intent": "GENERAL", # Force general description
    "telemetry": {
        "speed_mps": 0.0,
        "location_type": "INDOOR"
    },
    "language": "fa",
    "audio_query": "Describe this test image."
}

print(f"📡 Connecting to: {url}")
print(f"📤 Sending payload with intent: {payload['user_intent']}...")

try:
    req = urllib.request.Request(
        url,
        data=json.dumps(payload).encode('utf-8'),
        headers={'Content-Type': 'application/json'}
    )
    
    with urllib.request.urlopen(req) as response:
        status_code = response.getcode()
        body = response.read().decode('utf-8')
        
        print(f"\n✅ Success! Status Code: {status_code}")
        print("📥 Response from Cloud:")
        
        # Pretty print JSON
        try:
            parsed_json = json.loads(body)
            print(json.dumps(parsed_json, indent=2))
        except:
            print(body)

except urllib.error.HTTPError as e:
    print(f"\n❌ HTTP Error: {e.code}")
    print(e.read().decode('utf-8'))
except urllib.error.URLError as e:
    print(f"\n❌ Connection Error: {e.reason}")
except Exception as e:
    print(f"\n❌ Error: {e}")
