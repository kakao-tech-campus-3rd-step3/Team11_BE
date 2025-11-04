import os
from util.client import ApplicationClient
from util.fs import FileSystem
from util.logger import get_logger


class MemberGenerator:
    def __init__(self):
        ADMIN_EMAIL = os.getenv("ADMIN_EMAIL")
        ADMIN_PASSWORD = os.getenv("ADMIN_PASSWORD")
        if not ADMIN_EMAIL:
            raise ValueError("ADMIN_EMAIL가 설정되지 않았습니다!")
        if not ADMIN_PASSWORD:
            raise ValueError("ADMIN_PASSWORD가 설정되지 않았습니다!")
        
        self.admin_email = ADMIN_EMAIL
        self.admin_password = ADMIN_PASSWORD
        self.client = ApplicationClient()
        self.client.set_auth_by_email(self.admin_email, self.admin_password)
        self.test_testpass = "testpass1212!"
        self.fs = FileSystem()
        self.logger = get_logger("MemberGenerator")
        
    def generate_members(self, domain_name: str, count: int):
        self.logger.info(f"{domain_name} 회원 생성 시작")
        reses = []
        self.client.set_auth_by_email(self.admin_email, self.admin_password)
        for i in range(1, count + 1):
            res = self.client.post("api/members", {
                "email": f"{domain_name}{i:02d}@test.com",
                "password": self.test_testpass,
                "roles": ["ROLE_USER"]
            })
            if res.status_code != 201:
                self.logger.error(f"회원 생성 실패: {res.json()}")
                raise ValueError(f"회원 생성 실패: {res.json()}")
            
            reses.append(res.json())
        self.fs.save_responses(f"{domain_name}_members.json", reses)
        self.logger.info(f"회원 생성 완료: {len(reses)}개")
        
    def generate_auth_tokens(self, domain_name: str):
        self.logger.info("액세스 토큰 생성 시작")
        reses = []
        for res in self.fs.read_responses(f"{domain_name}_members.json"):
            email = res["email"]
            password = self.test_testpass
            res_auth = self.client.post("api/auth/login", {
                "email": email,
                "password": password
            })
            if res_auth.status_code != 200:
                self.logger.error(f"액세스 토큰 생성 실패: {res_auth.json()}")
            body = res_auth.json()
            reses.append({
                "memberId": res["id"],
                "email": email,
                "accessToken": body["accessToken"],
                "refreshToken": body["refreshToken"]
            })
        self.fs.save_responses(f"{domain_name}_auth_tokens.json", reses)
        self.logger.info(f"액세스 토큰 생성 완료: {len(reses)}개")
    
    def clear_members(self, domain_name: str):
        members = self.fs.read_responses(f"{domain_name}_members.json")
        for member in members:
            self.client.delete(f"api/members/{member['id']}")
        
        self.fs.delete_responses(f"{domain_name}_members.json")
        # db에서 연관되어 있는 객체들도 삭제됨
        self.fs.delete_responses(f"{domain_name}_auth_tokens.json") 
        self.fs.delete_responses(f"{domain_name}_profiles.json")

    def generate_profiles(self, domain_name: str):
        self.logger.info(f"{domain_name} 회원 프로필 생성 시작")
        auth_tokens = self.fs.read_responses(f"{domain_name}_auth_tokens.json")
        requests = self.fs.read_request(f"{domain_name}_profiles.json")
        
        responses = []
        for i, request in enumerate(requests):
            token = auth_tokens[i]["accessToken"]
            raw_client = self.client.get_raw_client()
            raw_client.headers.update({"Authorization": f"Bearer {token}"})

            request_copy = request.copy()
            image_filename = request_copy.pop("image")
            image_path = os.path.join(self.fs.request_path, "images", image_filename)
            
            if not os.path.exists(image_path):
                self.logger.error(f"이미지 파일을 찾을 수 없습니다: {image_path}")
                raise FileNotFoundError(f"이미지 파일을 찾을 수 없습니다: {image_path}")
            
            files = {}
            data = {}
            with open(image_path, "rb") as image_file:
                image_bytes = image_file.read()
                files["image"] = (image_filename, image_bytes, "image/png")
                for key, value in request_copy.items():
                    if value is not None:
                        data[key] = str(value)
                res = raw_client.post("api/profiles", files=files, data=data)
                if res.status_code != 201:
                    self.logger.error(f"프로필 생성 실패: {res.json()}")
                    raise ValueError(f"프로필 생성 실패: {res.json()}")
                responses.append(res.json())
        self.fs.save_responses(f"{domain_name}_profiles.json", responses)
        self.logger.info(f"{domain_name} 프로필 생성 완료: {len(responses)}개")