import json
import requests
import base64

# Get token
print("Step 1: Getting token...")
data = {'userId': 1, 'email': 'test@ttelgo.com', 'role': 'USER'}
token_resp = requests.post('http://localhost:8080/api/v1/auth/test/token', json=data)
token_data = token_resp.json()

if token_data.get('success'):
    token = token_data['data']['accessToken']
    print(f"✓ Token obtained: {token[:50]}...")
    
    # Decode token to see what's inside
    parts = token.split('.')
    if len(parts) >= 2:
        try:
            header = json.loads(base64.urlsafe_b64decode(parts[0] + '==').decode())
            payload = json.loads(base64.urlsafe_b64decode(parts[1] + '==').decode())
            print(f"Token algorithm: {header.get('alg')}")
            print(f"Token user ID: {payload.get('userId')}")
            print(f"Token email: {payload.get('email')}")
            print(f"Token type: {payload.get('type')}")
        except:
            pass
    
    print("\nStep 2: Testing bundles endpoint...")
    headers = {'Authorization': 'Bearer ' + token}
    bundles_resp = requests.get('http://localhost:8080/api/v1/bundles?size=1', headers=headers)
    print(f"Response status: {bundles_resp.status_code}")
    bundles_data = bundles_resp.json()
    print(f"Response: {json.dumps(bundles_data, indent=2)[:500]}")
    
    if bundles_data.get('success'):
        count = len(bundles_data.get('data', {}).get('bundles', []))
        print(f"\n✓✓✓ SUCCESS! Found {count} bundles!")
        print("\n" + "="*50)
        print("LIVE API ENDPOINTS:")
        print("="*50)
        print("\n1. Get Token:")
        print("   POST https://ttelgo.com/api/v1/auth/test/token")
        print('   Body: {"userId":1,"email":"test@ttelgo.com","role":"USER"}')
        print("\n2. List Bundles:")
        print("   GET https://ttelgo.com/api/v1/bundles")
        print(f"   Header: Authorization: Bearer {token[:50]}...")
    else:
        print(f"\n✗ Error: {bundles_data.get('message')}")
else:
    print(f"✗ Token error: {token_data.get('message')}")

