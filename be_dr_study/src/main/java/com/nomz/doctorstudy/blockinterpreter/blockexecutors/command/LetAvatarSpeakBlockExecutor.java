package com.nomz.doctorstudy.blockinterpreter.blockexecutors.command;

import com.nomz.doctorstudy.api.ExternalApiCallService;
import com.nomz.doctorstudy.moderator.VoiceType;
import com.nomz.doctorstudy.blockinterpreter.ProcessContext;
import com.nomz.doctorstudy.blockinterpreter.ProcessLockManager;
import com.nomz.doctorstudy.blockinterpreter.ThreadProcessContext;
import com.nomz.doctorstudy.blockinterpreter.blockexecutors.BlockExecutor;
import com.nomz.doctorstudy.common.audio.AudioUtils;
import com.nomz.doctorstudy.conference.room.RoomParticipantInfo;
import com.nomz.doctorstudy.conference.room.SignalUtils;
import com.nomz.doctorstudy.conference.room.signal.AvatarDialogueSignal;
import com.nomz.doctorstudy.conference.room.signal.AvatarSpeakSignal;
import com.nomz.doctorstudy.conference.room.SignalTransmitter;
import com.nomz.doctorstudy.conference.room.signal.GptSummarySignal;
import com.nomz.doctorstudy.conference.room.signal.NextStepSignal;
import com.nomz.doctorstudy.image.service.MediaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class LetAvatarSpeakBlockExecutor extends BlockExecutor {
    private final ThreadProcessContext threadProcessContext;
    private final SignalTransmitter signalTransmitter;
    private final ExternalApiCallService externalApiCallService;
    private final SignalUtils signalUtils;
    private final MediaService mediaService;

    @Value("${audio-utils.upper-path}")
    private String audioUpperPath;
    private static final String AUDIO_FILE_NAME = "avatar_audio_";
    private static final String AUDIO_EXT = ".mp3";

    public LetAvatarSpeakBlockExecutor(ThreadProcessContext threadProcessContext, SignalTransmitter signalTransMitter, ExternalApiCallService externalApiCallService, SignalUtils signalUtils, MediaService mediaService) {
        super(void.class, List.of(String.class));
        this.threadProcessContext = threadProcessContext;
        this.signalTransmitter = signalTransMitter;
        this.externalApiCallService = externalApiCallService;
        this.signalUtils = signalUtils;
        this.mediaService = mediaService;
    }

    @Override
    protected Object executeAction(List<Object> args) {
        String speechContent = (String) args.get(0);

        ProcessContext processContext = threadProcessContext.get();
        long processId = processContext.getId();

        VoiceType voiceType = processContext.getConferenceContext().getVoiceType();
        byte[] speechAudio = externalApiCallService.tts(speechContent, voiceType);
        log.debug("let avatar speak: {}", speechContent);

        String audioPath = audioUpperPath + AUDIO_FILE_NAME + processId + AUDIO_EXT;
        AudioUtils.saveFile(speechAudio, audioPath);
        File file = new File(audioPath);
        int audioDurationMills = AudioUtils.getAudioLength(file.getAbsolutePath());
        log.debug("tts audio duration={}", audioDurationMills);
        String audioUrl = mediaService.saveAudio(file);

        signalTransmitter.transmitSignal(processId, new AvatarDialogueSignal(speechContent));

        signalUtils.sendMuteSignals(processId, processContext.getParticipantInfoList().stream().map(RoomParticipantInfo::getMemberId).toList());
        signalTransmitter.transmitSignal(
                processId,
                new AvatarSpeakSignal(
                        audioDurationMills,
                        audioUrl
                )
        );

        log.debug("thread:{} started to sleep", processId);
        ProcessLockManager.sleep(processId, audioDurationMills);
        log.debug("thread:{} is being awaken", processId);

        signalUtils.sendUnmuteSignals(processId, processContext.getParticipantInfoList().stream().map(RoomParticipantInfo::getMemberId).toList());
        String summary = externalApiCallService.gpt("다음 문장을 맘껏 요약해서 말해봐 => " + speechContent);
        log.debug("summary text={}", summary);
        signalTransmitter.transmitSignal(processId, new GptSummarySignal(summary));

        signalTransmitter.transmitSignal(processId, new NextStepSignal());

        return null;
    }

    @Override
    public Object executeGetProgramme(List<Object> args) {
        threadProcessContext.get().addProgrammeItem(Map.of("nickname", "AI 사회자", "time", -1));

        return null;
    }
}
