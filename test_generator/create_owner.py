import os
from dotenv import load_dotenv
from generator.meetup import MeetupGenerator

def create_owner(meetup_generator: MeetupGenerator):
    meetup_generator.generate_meetups()
    
def clear_owner(meetup_generator: MeetupGenerator):
    meetup_generator.clear_owner()
    try:
        meetup_generator.clear_owner()
    except Exception as e:
        print(e)
        
if __name__ == "__main__":
    load_dotenv()
    
    base_url = os.getenv("API_SERVER_URL")
    admin_email = os.getenv("ADMIN_EMAIL")
    admin_password = os.getenv("ADMIN_PASSWORD")
    test_password = os.getenv("TEST_PASSWORD")
    if not base_url or not admin_email or not admin_password or not test_password:
        raise ValueError("환경변수가 제대로 설정되지 않았습니다!")
    meetup_generator = MeetupGenerator(base_url, admin_email, admin_password, test_password)
    
    clear_owner(meetup_generator)
    create_owner(meetup_generator)