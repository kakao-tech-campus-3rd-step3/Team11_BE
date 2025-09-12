package com.pnu.momeet.domain.meetup.service.mapper;

import com.pnu.momeet.domain.common.mapper.PageMapper;
import com.pnu.momeet.domain.meetup.dto.request.MeetupCreateRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupPageRequest;
import com.pnu.momeet.domain.meetup.dto.request.MeetupUpdateRequest;
import com.pnu.momeet.domain.meetup.entity.Meetup;
import com.pnu.momeet.domain.meetup.enums.MainCategory;
import com.pnu.momeet.domain.meetup.enums.SubCategory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.function.Consumer;

public class MeetupDtoMapper {
    private MeetupDtoMapper() {
        // private constructor to prevent instantiation
    }

    public static Meetup toEntity(MeetupCreateRequest request) {
        return Meetup.builder()
                .name(request.name())
                .hashTags(request.hashTags())
                .description(request.description())
                .capacity(request.capacity())
                .scoreLimit(request.scoreLimit())
                .address(request.location().address())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .build();
    }

    public static Consumer<Meetup> toConsumer(MeetupUpdateRequest request) {
       // locationPoint, sigungu는 서비스 레이어에서 설정
        return meetup -> {
            if (request.name() != null) {
                meetup.setName(request.name());
            }
            if (request.category() != null) {
                meetup.setCategory(MainCategory.valueOf(request.category()));
            }
            if (request.subCategory() != null) {
                meetup.setSubCategory(SubCategory.valueOf(request.subCategory()));
            }
            if (request.description() != null) {
                meetup.setDescription(request.description());
            }
            if (request.hashTags() != null) {
                meetup.setHashTags(request.hashTags());
            }
            if (request.capacity() != null) {
                meetup.setCapacity(request.capacity());
            }
            if (request.scoreLimit() != null) {
                meetup.setScoreLimit(request.scoreLimit());
            }
            if (request.startAt() != null) {
                meetup.setStartAt(request.startAt());
            }
            if (request.endAt() != null) {
                meetup.setEndAt(request.endAt());
            }
            if (request.location() != null) {
                meetup.setAddress(request.location().address());
            }
        };
    }

    public static PageRequest toPageRequest(MeetupPageRequest request) {
        return PageRequest.of(
                request.getPage(),
                request.getSize(),
                PageMapper.toSort(request.getSort())
        );
    }

    public static Specification<Meetup> toSpecification(MeetupPageRequest request) {
        Specification<Meetup> spec = (root, query, cb) -> cb.conjunction();
        if (request.getSigunguCode() != null) {
            spec = spec.and((root, query, cb)
                    -> cb.equal(root.get("sigungu").get("id"), request.getSigunguCode()));
        }
        if (request.getCategory() != null) {
            MainCategory category = MainCategory.valueOf(request.getCategory());
            spec = spec.and((root, query, cb)
                    -> cb.equal(root.get("category"), category));
        }
        if (request.getSubCategory() != null) {
            SubCategory subCategory = SubCategory.valueOf(request.getSubCategory());
            spec = spec.and((root, query, cb)
                    -> cb.equal(root.get("subCategory"), subCategory));
        }
        if (request.getSearch() != null && !request.getSearch().isEmpty()) {
            String pattern = "%" + request.getSearch().toLowerCase() + "%";
            spec = spec.and((root, query, cb)
                    -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            ));
        }
        return spec;
    }
}
