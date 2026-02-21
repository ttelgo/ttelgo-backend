import json
import requests

# Get token
print("Getting token...")
data = {'userId': 1, 'email': 'test@ttelgo.com', 'role': 'USER'}
token_resp = requests.post('http://localhost:8080/api/v1/auth/test/token', json=data)
token_data = token_resp.json()

if token_data.get('success'):
    token = token_data['data']['accessToken']
    print("\n" + "="*70)
    print("FULL JWT TOKEN:")
    print("="*70)
    print(token)
    print("="*70)
    print("\nToken Details:")
    print(f"  - User ID: {token_data['data']['userId']}")
    print(f"  - Email: {token_data['data']['email']}")
    print(f"  - Expires In: {token_data['data']['expiresIn']} seconds ({token_data['data']['expiresIn']/86400:.1f} days)")
    print("\n" + "="*70)
    print("USE THIS TOKEN IN POSTMAN:")
    print("="*70)
    print(f"Authorization: Bearer {token}")
    print("="*70)
    
    # Test bundles endpoint
    print("\nTesting bundles endpoint with this token...")
    headers = {'Authorization': 'Bearer ' + token}
    bundles_resp = requests.get('http://localhost:8080/api/v1/bundles?size=2', headers=headers)
    
    if bundles_resp.status_code == 200:
        bundles_data = bundles_resp.json()
        if bundles_data.get('success'):
            count = len(bundles_data.get('data', {}).get('bundles', []))
            print(f"✓ SUCCESS! Bundles API is working! Found {count} bundles")
        else:
            print(f"✗ Error: {bundles_data.get('message')}")
    else:
        print(f"✗ HTTP {bundles_resp.status_code}: {bundles_resp.text[:200]}")
else:
    print(f"✗ Failed to get token: {token_data.get('message')}")

