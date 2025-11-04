from dotenv import load_dotenv
from generator.member import MemberGenerator
from generator.meetup import MeetupGenerator

def create_test():
    meetup_generator = MeetupGenerator()
    meetup_generator.generate_meetups()
    meetup_generator.generate_participants()

# example/response 에 파일이 있을 때 실행
def clear_test():
    meetup_generator = MeetupGenerator()
    meetup_generator.clear_all()

if __name__ == "__main__":
    load_dotenv()

    try:
        clear_test()
    except Exception as e:
        print(e)
    create_test()