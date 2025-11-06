import json
import os
from util.logger import get_logger

class FileSystem:
    def __init__(self):
        self.base_path = "example"
        os.makedirs(os.path.join(self.base_path, "request"), exist_ok=True)
        os.makedirs(os.path.join(self.base_path, "response"), exist_ok=True)
        self.request_path = os.path.join(self.base_path, "request")
        self.response_path = os.path.join(self.base_path, "response")
        self.logger = get_logger("FileSystem")
    
    def read_request(self, domain_name: str, file_name: str) -> list[dict]:
        file_name = f"{domain_name}_{file_name}"
        if not os.path.exists(os.path.join(self.request_path, file_name)):
            self.logger.error(f"파일을 찾을 수 없습니다: {file_name}")
            raise FileNotFoundError(f"파일을 찾을 수 없습니다: {file_name}")
        
        with open(os.path.join(self.request_path, file_name), "r") as f:
            data = json.load(f)
            if "requests" not in data:
                self.logger.error(f"파일에 요청 데이터가 없습니다: {file_name}")
                raise ValueError(f"파일에 요청 데이터가 없습니다: {file_name}")
            self.logger.info(f"파일을 읽었습니다: {file_name}")
            return data["requests"]
    
    def read_responses(self, domain_name: str, file_name: str) -> list[dict]:
        file_path = os.path.join(self.response_path, domain_name, file_name)
        
        if not os.path.exists(file_path):
            self.logger.warning(f"파일을 찾을 수 없습니다: {file_name}")
            raise FileNotFoundError(f"파일을 찾을 수 없습니다: {file_name}")
        with open(file_path, "r") as f:
            data = json.load(f)
            self.logger.info(f"파일을 읽었습니다: {file_name}")
            return data["responses"]
    
    def save_responses(self, domain_name: str, file_name: str, data: list[dict]):
        folder_path = os.path.join(self.response_path, domain_name)
        os.makedirs(folder_path, exist_ok=True)
        file_path = os.path.join(folder_path, file_name)
        with open(file_path, "w", encoding="utf-8") as f:
            json.dump({"responses": data}, f, indent=4, ensure_ascii=False)
        self.logger.info(f"파일을 저장했습니다: {file_name}")
    
    def delete_responses(self, domain_name: str, file_name: str):
        folder_path = os.path.join(self.response_path, domain_name)
        os.makedirs(folder_path, exist_ok=True)
        file_path = os.path.join(folder_path, file_name)
        if not os.path.exists(file_path):
            self.logger.warning(f"파일을 찾을 수 없습니다: {file_name}")
            return
        os.remove(file_path)
        self.logger.info(f"파일을 삭제했습니다: {file_name}")