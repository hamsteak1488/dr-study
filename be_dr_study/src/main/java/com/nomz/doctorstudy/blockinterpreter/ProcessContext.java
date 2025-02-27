package com.nomz.doctorstudy.blockinterpreter;

import com.nomz.doctorstudy.blockinterpreter.blockexecutors.BlockVariable;
import com.nomz.doctorstudy.blockinterpreter.programme.ProgrammeItem;
import com.nomz.doctorstudy.common.exception.BusinessException;
import com.nomz.doctorstudy.conference.room.RoomParticipantInfo;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class ProcessContext {
    @Getter
    private final long id;
    @Getter @Setter
    private int cursor;
    @Getter @Setter
    private ProcessStatus status;
    private int scopeDepth;

    private final List<Block> commandBlocks;
    private final Map<String, Object> initVariableMap;
    private final Map<String, Integer> labelMap;
    private final List<Map<String, Object>> variableMapStack = new ArrayList<>();
    private final List<Transcript> transcripts = new ArrayList<>();

    @Getter
    private final GptContext gptContext;

    @Getter
    private final List<ProgrammeItem> programme = new ArrayList<>();

    @Getter
    private final List<RoomParticipantInfo> participantInfoList = new ArrayList<>();

    @Getter
    private final ConferenceContext conferenceContext;

    public ProcessContext(long id, List<Block> commandBlocks, Map<String, Object> initVarMap, Map<String, Integer> labelMap, ConferenceContext conferenceContext) {
        this.id = id;
        this.commandBlocks = commandBlocks;
        this.initVariableMap = new HashMap<>(initVarMap);
        this.labelMap = new HashMap<>(labelMap);
        this.gptContext = new GptContext();
        this.conferenceContext = conferenceContext;

        this.participantInfoList.add(new RoomParticipantInfo(0L, "paddingMember", "Padding Member PeerId"));
        this.participantInfoList.addAll(conferenceContext.getParticipantInfoList());
    }

    public void initialize() {
        this.status = ProcessStatus.READY;
        this.cursor = 0;
        this.scopeDepth = 0;

        this.variableMapStack.clear();
        this.variableMapStack.add(new HashMap<>(initVariableMap));

        this.transcripts.clear();
        this.programme.clear();

        initParticipantInfo();
    }

    private void initParticipantInfo() {
        int seq = 1;
        for (RoomParticipantInfo participantInfo : participantInfoList) {
            String variableName = BlockVariable.PARTICIPANT_NAME.getToken() + seq++;
            declareVariable(variableName);
            setVariable(variableName, participantInfo.getName());
        }
        declareVariable(BlockVariable.NUM_OF_PARTICIPANT.getToken());
        setVariable(BlockVariable.NUM_OF_PARTICIPANT.getToken(), participantInfoList.size() - 1);
    }

    public Integer getNumOfParticipant() {
        return participantInfoList.size() - 1;
    }

    public String getParticipantName(int numOfParticipant) {
        return participantInfoList.get(numOfParticipant).getName();
    }

    public Long getParticipantMemberId(int numOfParticipant) {
        return participantInfoList.get(numOfParticipant).getMemberId();
    }

    public void increaseCursor() {
        cursor++;
    }

    public void increaseScopeDepth() {
        variableMapStack.add(new HashMap<>());
        ++scopeDepth;
    }

    public void decreaseScopeDepth() {
        variableMapStack.remove(scopeDepth--);
    }

    public void declareVariable(String key) {
        if (variableMapStack.get(scopeDepth).containsKey(key)) {
            log.warn("This variable:{} has already been declared once", key);
            return;
            //throw new BlockException(BlockErrorCode.VARIABLE_ALREADY_DECLARED);
        }
        variableMapStack.get(scopeDepth).put(key, null);
    }

    public Object getVariable(String key) {
        for (int i = scopeDepth; i>=0; i--) {
            Object val = variableMapStack.get(i).get(key);
            if (val != null) {
                return val;
            }
        }
        return null;
    }

    public void setVariable(String key, Object val) {
        for (int i = scopeDepth; i>=0; i--) {
            if (variableMapStack.get(i).containsKey(key)) {
                variableMapStack.get(i).replace(key, val);
                return;
            }
        }
        variableMapStack.get(scopeDepth).put(key, val);
    }

    public void addTranscript(Long memberId, String content) {
        int currentPhase = (int) getVariable("current_phase");
        String participantName = participantInfoList.stream()
                .filter(info -> info.getMemberId() == memberId)
                .map(RoomParticipantInfo::getName)
                .findAny()
                .orElseThrow(() -> new BusinessException(BlockErrorCode.PARTICIPANT_NAME_NOT_FOUND));

        transcripts.add(new Transcript(participantName, currentPhase, content));
    }

    public Transcript getRecentTranscript(int n) {
        if (n < 1 || n > transcripts.size()) {
            throw new BlockException(BlockErrorCode.TRANSCRIPT_INDEX_OUT_OF_BOUND);
        }
        return transcripts.get(transcripts.size() - n);
    }

    public List<Transcript> getPhaseTranscript(int phase) {
        return transcripts.stream()
                .filter(transcript -> transcript.getPhase() == phase)
                .toList();
    }

    public int getLabelIndex(String name) {
        return labelMap.get(name);
    }

    public void setLabelIndex(String name, int index) {
        labelMap.put(name, index);
    }

    public boolean isEndOfBlock() {
        return cursor == commandBlocks.size();
    }

    public Block currentBlock() {
        return commandBlocks.get(cursor);
    }

    public void addProgrammeItem(Map<String, Object> content) {
        int currentPhase = (int) getVariable("current_phase");
        programme.add(new ProgrammeItem(currentPhase, content));
    }

    public void addGptHistory(String query, String answer) {
        gptContext.addHistory(query, answer);
    }


    public static class GptContext {
        private final StringBuilder history = new StringBuilder();

        public void addHistory(String query, String answer) {
            history.append("My Query=[").append(query).append("], Gpt Answer=[").append(answer).append("]");
        }

        public String getHistory() {
            return history.toString();
        }
    }
}
