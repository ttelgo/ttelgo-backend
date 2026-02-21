import json
import requests

# Get token
data = {'userId': 1, 'email': 'test@ttelgo.com', 'role': 'USER'}
token_resp = requests.post('http://localhost:8080/api/v1/auth/test/token', json=data)
token_data = token_resp.json()

if token_data.get('success'):
    token = token_data['data']['accessToken']
    print('✓ Token obtained')
    
    # Test bundles
    headers = {'Authorization': 'Bearer ' + token}
    bundles_resp = requests.get('http://localhost:8080/api/v1/bundles?size=2', headers=headers)
    bundles_data = bundles_resp.json()
    
    if bundles_data.get('success'):
        count = len(bundles_data.get('data', {}).get('bundles', []))
        print('✓✓✓ SUCCESS! Bundles API is WORKING! ✓✓✓')
        print(f'Found {count} bundles')
        print()
        print('========================================')
        print('LIVE API ENDPOINTS:')
        print('========================================')
        print()
        print('1. Get Token:')
        print('   POST https://ttelgo.com/api/v1/auth/test/token')
        print('   Body: {"userId":1,"email":"test@ttelgo.com","role":"USER"}')
        print()
        print('2. List Bundles:')
        print('   GET https://ttelgo.com/api/v1/bundles')
        print('   Header: Authorization: Bearer', token[:50] + '...')
    else:
        print('✗ Bundles error:', bundles_data.get('message'))
        print('Status:', bundles_resp.status_code)
else:
    print('✗ Token error:', token_data.get('message'))

