import datetime
from util.client import ApplicationClient
from util.fs import FileSystem
from util.logger import get_logger
from generator.member import MemberGenerator

class MeetupGenerator:
    def __init__(self):
        self.client = ApplicationClient()
        self.fs = FileSystem()
        self.logger = get_logger("MeetupGenerator")
        self.member_generator = MemberGenerator()

    def generate_meetups(self):
        self.logger.info("사전 데이터 생성 시작")
        self.member_generator.generate_members("owner", 10) # 소유자 10명 생성
        self.member_generator.generate_auth_tokens("owner")
        self.member_generator.generate_profiles("owner") # 소유자 10명 프로필 생성
        self.logger.info("사전 데이터 생성 완료")

        meetup_requests = self.member_generator.fs.read_request("meetup.json")
        owner_auth_tokens = self.member_generator.fs.read_responses("owner_auth_tokens.json") # 소유자 10명 액세스 토큰 읽기
        
        reponses = []
        
        self.logger.info("모임 생성 시작")
        for i, meetup_request in enumerate(meetup_requests):
            meetup_request_copy = meetup_request.copy()
            meetup_request_copy.pop("startAtTime")
            meetup_request_copy.pop("endAtTime")
            tomorrow = datetime.datetime.now() + datetime.timedelta(days=1)
            meetup_request_copy["startAt"] = f"{tomorrow.date()}T{meetup_request['startAtTime']}"
            meetup_request_copy["endAt"] = f"{tomorrow.date()}T{meetup_request['endAtTime']}"
            owner_auth_token = owner_auth_tokens[i] # 소유자 10명 중 한 명의 액세스 토큰 읽기
            
            self.client.set_auth_by_access_token(owner_auth_token["accessToken"]) # 액세스 토큰 설정
            res = self.client.post(f"api/meetups", meetup_request_copy) # 모임 생성
            
            if res.status_code != 201:
                self.logger.error(f"모임 생성 요청: {meetup_request_copy}")
                self.logger.error(f"모임 생성 실패: {res.json()}")
                raise ValueError(f"모임 생성 실패: {res.json()}")
            reponses.append(res.json())
        
        self.logger.info("모임 생성 완료")
        self.fs.save_responses("meetup_responses.json", reponses)
        self.logger.info("모임 생성 결과 저장 완료")
    
    def generate_participants(self):
        self.logger.info("참여자 생성 시작")
        self.member_generator.generate_members("part", 20) # 참여자 10명 생성
        self.member_generator.generate_auth_tokens("part")
        self.member_generator.generate_profiles("part") # 참여자 10명 프로필 생성
        self.logger.info("참여자 생성 완료")
    
    
    def clear_all(self):
        self.member_generator.clear_members("owner") # member를 삭제하면 연관되어 있는 객체들도 삭제됨
        self.member_generator.clear_members("part") # member를 삭제하면 연관되어 있는 객체들도 삭제됨
        self.fs.delete_responses("meetup_responses.json") # 모임 생성 결과 삭제
        self.logger.info("모든 데이터 삭제 완료")