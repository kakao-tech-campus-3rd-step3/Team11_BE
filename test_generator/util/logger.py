import logging
from typing import Optional


def get_logger(name: Optional[str] = None):
    logger = logging.getLogger(name or __name__)
    logger.setLevel(logging.INFO)
    
    # 이미 핸들러가 있으면 추가하지 않음
    if logger.handlers:
        return logger
    
    # 콘솔 출력용 핸들러
    handler = logging.StreamHandler()
    handler.setLevel(logging.INFO)
    
    # 포맷터 설정
    formatter = logging.Formatter(
        '%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S'
    )
    handler.setFormatter(formatter)
    
    logger.addHandler(handler)
    return logger