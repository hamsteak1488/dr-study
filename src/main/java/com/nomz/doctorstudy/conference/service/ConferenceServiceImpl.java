package com.nomz.doctorstudy.conference.service;

import com.nomz.doctorstudy.common.exception.BusinessException;
import com.nomz.doctorstudy.common.exception.CommonErrorCode;
import com.nomz.doctorstudy.conference.entity.Conference;
import com.nomz.doctorstudy.conference.ConferenceErrorCode;
import com.nomz.doctorstudy.conference.dto.ConferenceSearchFilter;
import com.nomz.doctorstudy.conference.entity.ConferenceMember;
import com.nomz.doctorstudy.conference.entity.ConferenceMemberInvite;
import com.nomz.doctorstudy.conference.repository.ConferenceMemberInviteRepository;
import com.nomz.doctorstudy.conference.repository.ConferenceMemberRepository;
import com.nomz.doctorstudy.conference.repository.ConferenceQueryRepository;
import com.nomz.doctorstudy.conference.repository.ConferenceRepository;
import com.nomz.doctorstudy.conference.request.CreateConferenceRequest;
import com.nomz.doctorstudy.conference.request.GetConferenceListRequest;
import com.nomz.doctorstudy.conference.request.InviteMemberConferenceRequest;
import com.nomz.doctorstudy.conference.request.JoinConferenceRequest;
import com.nomz.doctorstudy.conference.response.*;
import com.nomz.doctorstudy.conference.room.RoomService;
import com.nomz.doctorstudy.member.entity.Member;
import com.nomz.doctorstudy.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConferenceServiceImpl implements ConferenceService {
    private final ConferenceRepository conferenceRepository;
    private final ConferenceQueryRepository conferenceQueryRepository;
    private final RoomService roomService;
    private final ConferenceMemberRepository conferenceMemberRepository;
    private final ConferenceMemberInviteRepository conferenceMemberInviteRepository;
    private final MemberRepository memberRepository;

    private final ConcurrentHashMap<Long, ReentrantLock> joinLockMap = new ConcurrentHashMap<>();

    @Override
    public CreateConferenceResponse createConference(/*Member requester, */CreateConferenceRequest request) {
        Conference conference = Conference.builder()
                .title(request.getTitle())
                //.host(requester)
                .memberCapacity(request.getMemberCapacity())
                .build();
        conferenceRepository.save(conference);

        log.info("[new conference] id={}, title={}", conference.getId(), conference.getTitle());

        return CreateConferenceResponse.builder()
                .conferenceId(conference.getId())
                .build();
    }

    @Override
    @Transactional
    public GetConferenceResponse getConference(Long conferenceId) {
        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new BusinessException(ConferenceErrorCode.CONFERENCE_NOT_FOUND_ERROR));

        return GetConferenceResponse.builder()
                .id(conference.getId())
                .title(conference.getTitle())
                .memberCapacity(conference.getMemberCapacity())
                .build();
    }

    @Override
    public List<GetConferenceListResponse> getConferenceList(GetConferenceListRequest request) {
        List<Conference> conferenceList = conferenceQueryRepository.getConferenceList(
                ConferenceSearchFilter.builder()
                        .title(request.getTitle())
                        .memberCapacity(request.getMemberCapacity())
                        .build()
        );

        return conferenceList.stream()
                .map(GetConferenceListResponse::of)
                .toList();
    }

    @Override
    public List<GetConferenceParticipantListResponseItem> getConferenceParticipantList(Long conferenceId) {
        List<ConferenceMember> conferenceMembers = conferenceMemberRepository.findByConferenceId(conferenceId);

        return conferenceMembers.stream()
                .map(ConferenceMember::getMember)
                .map(GetConferenceParticipantListResponseItem::of)
                .toList();
    }

    @Override
    public void startConference(Long conferenceId) {
        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new BusinessException(ConferenceErrorCode.CONFERENCE_NOT_FOUND_ERROR));

        if (joinLockMap.containsKey(conferenceId)) {
            throw new BusinessException(ConferenceErrorCode.CONFERENCE_ALREADY_STARTED);
        }
        joinLockMap.put(conferenceId, new ReentrantLock());
        roomService.createRoom(conferenceId);
    }

    @Override
    public void finishConference(Long conferenceId) {
        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new BusinessException(ConferenceErrorCode.CONFERENCE_NOT_FOUND_ERROR));

        if (!joinLockMap.containsKey(conferenceId)) {
            throw new BusinessException(ConferenceErrorCode.CONFERENCE_NOT_IN_PROCESS);
        }

        roomService.removeRoom(conferenceId);
        joinLockMap.remove(conferenceId);
    }

    @Override
    @Transactional
    public JoinConferenceResponse joinConference(/*Member requester, */Long conferenceId, JoinConferenceRequest request) {
        List<String> peerIds;
        ReentrantLock lock = joinLockMap.get(conferenceId);
        if (lock == null) {
            throw new BusinessException(ConferenceErrorCode.CONFERENCE_NOT_IN_PROCESS);
        }

        lock.lock();
        try {
            peerIds = roomService.getPeerList(conferenceId);
            roomService.addPeer(conferenceId, request.getPeerId());
        } finally {
            lock.unlock();
        }

        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new BusinessException(ConferenceErrorCode.CONFERENCE_NOT_FOUND_ERROR));

        /*
        ConferenceMember conferenceMember = ConferenceMember.builder()
                .id(new ConferenceMember.ConferenceMemberId(conferenceId, requester.getId()))
                .conference(conference)
                //.member(requester)
                .build();
        conferenceMemberRepository.save(conferenceMember);
        */

        return JoinConferenceResponse.builder()
                .existingPeerIds(peerIds)
                .build();
    }

    @Override
    @Transactional
    public InviteMemberConferenceResponse inviteMemberConference(Member requester, Long conferenceId, InviteMemberConferenceRequest request) {
        Conference conference = conferenceRepository.findById(conferenceId)
                .orElseThrow(() -> new BusinessException(ConferenceErrorCode.CONFERENCE_NOT_FOUND_ERROR));

        if (requester.getId() != conference.getHost().getId()) {
            // TODO: MemberErrorCode로 변경
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }

        // TODO: Member 에러코드 변경
        Long inviteeId = request.getInviteeId();
        Member member = memberRepository.findById(inviteeId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BAD_REQUEST));

        ConferenceMemberInvite conferenceMemberInvite = ConferenceMemberInvite.builder()
                .id(new ConferenceMemberInvite.ConferenceMemberInviteId(conferenceId, inviteeId))
                .conference(conference)
                .member(member)
                .build();
        conferenceMemberInviteRepository.save(conferenceMemberInvite);

        return InviteMemberConferenceResponse.builder()
                .build();
    }
}
