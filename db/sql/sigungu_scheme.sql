-- Table: public.sigungu_boundary

-- DROP TABLE IF EXISTS public.sigungu_boundary;

CREATE TABLE IF NOT EXISTS public.sigungu_boundary
(
    sgg_code bigint NOT NULL,
    sido_code bigint NOT NULL,
    sido_name character varying(255) COLLATE pg_catalog."default" NOT NULL,
    sgg_name character varying(255) COLLATE pg_catalog."default" NOT NULL,
    geom geometry(Polygon,4326) NOT NULL,
    base_location geometry(Point,4326) NOT NULL,
    created_at timestamp without time zone NOT NULL DEFAULT now(),
    updated_at timestamp without time zone NOT NULL DEFAULT now(),
    CONSTRAINT sigungu_boundary_pkey PRIMARY KEY (sgg_code)
);

COMMENT ON TABLE public.sigungu_boundary
    IS '한국 시군구 경계 데이터 (Polygon, 대표점 포함)';

COMMENT ON COLUMN public.sigungu_boundary.sgg_code
    IS '시군구코드';

COMMENT ON COLUMN public.sigungu_boundary.sido_code
    IS '시도코드';

COMMENT ON COLUMN public.sigungu_boundary.sido_name
    IS '시도명';

COMMENT ON COLUMN public.sigungu_boundary.sgg_name
    IS '시군구명';

COMMENT ON COLUMN public.sigungu_boundary.geom
    IS '경계 기하정보 (WGS84 좌표계, Polygon)';

COMMENT ON COLUMN public.sigungu_boundary.base_location
    IS '대표 좌표 (WGS84 좌표계, Point)';
-- Index: idx_sigungu_boundary_geom

-- DROP INDEX IF EXISTS public.idx_sigungu_boundary_geom;

CREATE INDEX IF NOT EXISTS idx_sigungu_boundary_geom
    ON public.sigungu_boundary USING gist
        (geom);
-- Index: idx_sigungu_boundary_sido_code

-- DROP INDEX IF EXISTS public.idx_sigungu_boundary_sido_code;

CREATE INDEX IF NOT EXISTS idx_sigungu_boundary_sido_code
    ON public.sigungu_boundary USING btree
        (sido_code ASC NULLS LAST);
