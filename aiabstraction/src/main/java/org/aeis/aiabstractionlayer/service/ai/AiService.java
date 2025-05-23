package org.aeis.aiabstractionlayer.service.ai;

import lombok.RequiredArgsConstructor;
import org.aeis.aiabstractionlayer.dto.RecordingDTO;
import org.aeis.aiabstractionlayer.kafka.AiProducer;
import org.aeis.aiabstractionlayer.payload.LectureSummaryRequestPayload;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AiService {

    private final AiProducer aiProducer;

    public void startRecording(RecordingDTO recordingDTO) {
        aiProducer.sendRecordControl("start_recording",recordingDTO.getContextFile(),recordingDTO.getCourseId());
    }

    public void stopRecording() {
        aiProducer.sendRecordControl("stop_recording");
    }

    /**
     * Request a lecture summary from Python,
     * returning immediately (async). The summary
     * will come back in 'lecture_summary_responses'
     */
    public String requestLectureSummary(String transcript, byte[] contextFile) {
        String reqId = UUID.randomUUID().toString();
        String contextBase64 = null;
        if (contextFile != null && contextFile.length > 0) {
            contextBase64 = java.util.Base64.getEncoder().encodeToString(contextFile);
        }

        LectureSummaryRequestPayload request = LectureSummaryRequestPayload.builder()
                .requestId(reqId)
                .transcript(transcript)
                .contextBase64(contextBase64)
                .build();

        aiProducer.sendLectureSummaryRequest(request);
        return reqId; // you can track the requestId, then handle the response in AiConsumer
    }
}
