-- 시군구 경계(sigungu_boundary) 테이블 생성 스크립트
-- 작성일: 2025년
-- 설명: 한국 시군구 경계 데이터 저장을 위한 테이블 (Polygon + 대표점)

-- PostGIS 확장 기능이 설치되어 있는지 확인
CREATE EXTENSION IF NOT EXISTS postgis;

-- 기존 테이블이 있다면 삭제 (주의: 기존 데이터가 삭제됩니다)
DROP TABLE IF EXISTS sigungu_boundary CASCADE;

-- 시군구 경계 테이블 생성
CREATE TABLE sigungu_boundary (
    id BIGINT PRIMARY KEY,                    -- 시군구코드
    sido_code BIGINT NOT NULL,                   -- 시도코드
    sgg_name VARCHAR(255) NOT NULL,              -- 시군구명
    sido_name VARCHAR(255) NOT NULL,             -- 시도명
    geom GEOMETRY(POLYGON, 4326) NOT NULL,       -- 경계 기하정보 (WGS84, Polygon)
    base_location GEOMETRY(POINT, 4326) NOT NULL,-- 기준점(대표 좌표)
    created_at TIMESTAMP NOT NULL DEFAULT NOW(), -- 생성일시
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()  -- 수정일시
);

-- 공간 인덱스 생성 (검색 성능 향상)
CREATE INDEX idx_sigungu_boundary_geom ON sigungu_boundary USING GIST (geom);

-- 시군구코드 인덱스 생성
CREATE INDEX idx_sigungu_boundary_sgg_code ON sigungu_boundary (sgg_code);

-- 테이블 및 컬럼 코멘트 추가
COMMENT ON TABLE sigungu_boundary IS '한국 시군구 경계 데이터 (Polygon, 대표점 포함)';
COMMENT ON COLUMN sigungu_boundary.id IS '기본키';
COMMENT ON COLUMN sigungu_boundary.sido_name IS '시도명';
COMMENT ON COLUMN sigungu_boundary.sido_code IS '시도코드';
COMMENT ON COLUMN sigungu_boundary.sgg_name IS '시군구명';
COMMENT ON COLUMN sigungu_boundary.sgg_code IS '시군구코드';
COMMENT ON COLUMN sigungu_boundary.geom IS '경계 기하정보 (WGS84 좌표계, Polygon)';
COMMENT ON COLUMN sigungu_boundary.base_location IS '대표 좌표 (WGS84 좌표계, Point)';

-- 테이블 생성 완료 메시지
SELECT 'sigungu_boundary 테이블이 성공적으로 생성되었습니다.' AS message;

-- 좌표계 정보 확인
SELECT 
    f_table_name,
    f_geometry_column,
    coord_dimension,
    srid,
    type
FROM geometry_columns 
WHERE f_table_name = 'sigungu_boundary';


