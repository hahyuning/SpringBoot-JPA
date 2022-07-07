package jpabook.jpashop.api;

import jpabook.jpashop.domain.Member;
import jpabook.jpashop.service.MemberService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.stream.Collectors;

// @Controller @ResponseBody
@RestController
@RequiredArgsConstructor
public class MemberApiController {

    private final MemberService memberService;

    /**
     * 등록 v1: 요청으로 엔티티를 직접 받는 방법
     *
     *  문제점
     *  1. 엔티티에 프레젠테이션 계층을 위한 로직이 추가된다. (ex, api 검증을 위한 로직)
     *  2. 실무에서는 회원 엔티티를 위한 api가 다양하게 만들어지는데, 한 엔티티에 각각의 api를 위한 모든 요청 요구사항을 담기는 어렵다.
     *
     *  결론: api 요청 스펙에 맞추어 별도의 dto를 파라미터로 받는다.
     */
    @PostMapping("/api/v1/members")
    public CreateMemberResponse saveMemberV1(@RequestBody @Valid Member member) {
        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    /*
     * 등록 v2: 요청 값으로 엔티티 대신 별도의 dto를 받는 방법
     *
     * 장점
     *  1. 엔티티와 프레젠테이션 계층을 위한 로직을 분리할 수 있다.
     *  2. 엔티티와 api 스펙을 명확하게 분리할 수 있다. 즉, 엔티티가 변해도 api 스펙이 변하지 않는다.
     */
    @PostMapping("/api/v2/members")
    public CreateMemberResponse saveMemberV2(@RequestBody @Valid CreateMemberRequest request) {
        Member member = new Member();
        member.setName(request.getName());

        Long id = memberService.join(member);
        return new CreateMemberResponse(id);
    }

    @Data
    static class CreateMemberRequest {
        @NotEmpty
        private String name;
    }

    @Data
    static class CreateMemberResponse {
        private Long id;

        public CreateMemberResponse(Long id) {
            this.id = id;
        }
    }

    /**
     * 수정: dto를 요청 파라미터에 매핑한 후 변경감지를 사용해서 데이터를 수정
     */
    @PostMapping("/api/v2/members/{id}")
    public UpdateMemberResponse updateMemberV2(
            @PathVariable("id") Long id,
            @RequestBody @Valid UpdateMemberRequest request) {

        memberService.update(id, request.getName());
        Member findMember = memberService.findOne(id);
        return new UpdateMemberResponse(findMember.getId(), findMember.getName());
    }

    @Data
    static class UpdateMemberRequest {
        private String name;
    }

    @Data
    @AllArgsConstructor
    static class UpdateMemberResponse {
        private Long id;
        private String name;
    }

    /**
     * 조회 v1: 응답 값으로 엔티티를 직접 외부에 반환
     *
     * 문제점
     * 1. 엔티티에 프레젠테이션 계층을 위한 로직이 추가된다.
     * 2. 엔티티의 모든 값이 외부에 노출된다.
     * 3. 응답 스펙을 맞추기 위한 로직이 추가된다. (@JsonIgnore, 별도의 뷰 로직 등)
     * 4. 실무에서는 회원 엔티티를 위한 api가 다양하게 만들어지는데, 한 엔티티에 각각의 api를 위한 모든 요청 요구사항을 담기는 어렵다.
     * 5. 엔티티가 변경되면 api 스펙이 변한다.
     * 6. 추가로 컬렉션을 직접 반환하면 향후 api 스펙을 변경하기 어렵다. (볃로의 Result 클래스 생성으로 해결)
     *
     *  결론: api 응답 스펙에 맞추어 별도의 dto를 반환한다.
     */
    @GetMapping("/api/v1/members")
    public List<Member> membersV1() {
        return memberService.findMembers();
    }

    /**
     * 조회 v2: 응답 값으로 엔티티가 아닌 별도의 dto를 반환
     *
     * 장점
     * 1. 엔티티가 변해도 api 스펙이 변경되지 않는다.
     * 2. Result 클래스로 컬렉션을 감싸서 향후 필요한 필드를 추가할 수 있다. -> 유연한 설계 가능
     * */
    @GetMapping("/api/v2/members")
    public Result membersV2() {

        List<Member> findMembers = memberService.findMembers();
        // 엔티티 -> dto 반환
        List<MemberDto> collect = findMembers.stream()
                .map(m -> new MemberDto(m.getName()))
                .collect(Collectors.toList());

        return new Result(collect.size(), collect);
    }

    @Data
    @AllArgsConstructor
    static class Result<T> {
        private int count;
        private T data;
    }

    @Data
    @AllArgsConstructor
    static class MemberDto {
        private String name;
    }
}
