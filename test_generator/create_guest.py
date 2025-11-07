import os
from dotenv import load_dotenv
from generator.member import MemberGenerator
from generator.meetup import MeetupGenerator

def create_default_profiles(member_generator: MemberGenerator):
    member_generator.generate_default_profile("professor", 20)
    member_generator.generate_default_profile("team1", 10, 20)
    member_generator.generate_default_profile("team2", 10, 30)
    member_generator.generate_default_profile("team3", 10, 40)
    member_generator.generate_default_profile("team4", 10, 50)
    member_generator.generate_default_profile("team5", 10, 60)

def _clear_members(member_generator: MemberGenerator, prefix: str):
    try:
        member_generator.clear_members(prefix)
    except Exception as e:
        print(e)

def clear_default_profiles(member_generator: MemberGenerator):
    _clear_members(member_generator, "professor")
    _clear_members(member_generator, "team1")
    _clear_members(member_generator, "team2")
    _clear_members(member_generator, "team3")
    _clear_members(member_generator, "team4")
    _clear_members(member_generator, "team5")

if __name__ == "__main__":
    load_dotenv()
    
    base_url = os.getenv("API_SERVER_URL")
    admin_email = os.getenv("ADMIN_EMAIL")
    admin_password = os.getenv("ADMIN_PASSWORD")
    test_password = os.getenv("TEST_PASSWORD")
    if not base_url or not admin_email or not admin_password or not test_password:
        raise ValueError("환경변수가 제대로 설정되지 않았습니다!")
    meetup_generator = MeetupGenerator(base_url, admin_email, admin_password, test_password)
    member_generator = MemberGenerator(base_url, admin_email, admin_password, test_password)

    clear_default_profiles(member_generator)
    create_default_profiles(member_generator)