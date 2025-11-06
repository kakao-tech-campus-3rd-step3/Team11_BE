import httpx
import os

class ApplicationClient:
    
    def __init__(self, base_url: str):
        self.base_url = base_url
        self.client = httpx.Client(base_url=self.base_url)
        self.is_authenticated = False

    def _validate_authentication(self):
        if not self.is_authenticated:
            raise ValueError("인증되지 않았습니다!")

    def set_auth_by_email(self, email: str, password: str):
        res = self.client.post(f"/api/auth/login", json={"email": email, "password": password})
        if res.status_code != 200:
            raise ValueError(f"로그인 실패: {res.json()}")
        self.access_token = res.json()["accessToken"]
        self.client.headers.update({"Authorization": f"Bearer {self.access_token}"})
        self.is_authenticated = True
    
    def set_auth_by_access_token(self, access_token: str):
        if not access_token:
            raise ValueError("액세스 토큰이 설정되지 않았습니다!")
        self.client.headers.update({"Authorization": f"Bearer {access_token}"})
        self.is_authenticated = True

    def get(self, url: str):
        self._validate_authentication()
        return self.client.get(url)
    
    def post(self, url: str, body: dict):
        self._validate_authentication()
        return self.client.post(url, json=body)
    
    def delete(self, url: str):
        self._validate_authentication()
        return self.client.delete(url)
    
    def get_raw_client(self):
        return self.client