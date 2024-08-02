import { Button } from '@/components/atoms';
import React, { useState, useRef, useEffect } from 'react';

interface RecorderProps {
    memberId: number;
    conferenceId: number;
    stompClient: any;
    timeForAudioRecord: number;
    setTimeForAudioRecord: React.Dispatch<React.SetStateAction<number>>;
    isStartRecordingAudio: boolean;
}

function Recorder({
    memberId,
    conferenceId,
    stompClient,
    timeForAudioRecord,
    setTimeForAudioRecord,
    isStartRecordingAudio,
}: RecorderProps) {
    const [audioStream, setAudioStream] = useState<MediaStream | null>(null);
    const [isRecording, setIsRecording] = useState(false);
    const mediaRecorderRef = useRef<MediaRecorder | null>(null);
    const chunksRef = useRef<Blob[]>([]);

    useEffect(() => {
        if (timeForAudioRecord) {
            startAudioStream();

            // 설정한 시간 후에 오디오 녹음 중지
            const timeout = setTimeout(() => {
                stopAudioStream();
            }, timeForAudioRecord * 1000);

            return () => {
                clearTimeout(timeout);
                stopAudioStream();
                setTimeForAudioRecord(0);
            };
        }
    }, [isStartRecordingAudio]);

    // 오디오 스트림 시작
    const startAudioStream = async () => {
        console.log('start audio stream:', stompClient);

        try {
            const stream = await navigator.mediaDevices.getUserMedia({
                audio: true,
            });
            setAudioStream(stream);

            const mediaRecorder = new MediaRecorder(stream, {
                mimeType: 'audio/webm',
            });
            mediaRecorderRef.current = mediaRecorder;

            mediaRecorder.ondataavailable = (event) => {
                if (event.data.size > 0) {
                    chunksRef.current.push(event.data);
                }
            };

            mediaRecorder.onstop = () => {
                if (chunksRef.current.length > 0) {
                    const blob = new Blob(chunksRef.current, {
                        type: 'audio/webm',
                    });
                    chunksRef.current = []; // Clear chunks
                    const reader = new FileReader();
                    reader.onload = () => {
                        const arrayBuffer = reader.result as ArrayBuffer;

                        // ArrayBuffer를 Base64로 인코딩
                        const base64String = btoa(
                            new Uint8Array(arrayBuffer).reduce(
                                (data, byte) =>
                                    data + String.fromCharCode(byte),
                                '',
                            ),
                        );
                        // STOMP를 통해 오디오 전송
                        if (stompClient) {
                            stompClient.send(
                                `/pub/signal/${conferenceId}/participant-audio`, // 적절한 STOMP 경로 설정
                                {},
                                JSON.stringify({
                                    id: memberId,
                                    rawAudio: base64String as string, // ArrayBuffer로 전송
                                }),
                            );
                        }
                    };
                    reader.readAsArrayBuffer(blob); // ArrayBuffer로 변환
                }
            };

            mediaRecorder.start();
            setIsRecording(true);
            console.log('Audio recording started');
        } catch (error) {
            console.error('Error accessing audio stream:', error);
        }
    };

    const stopAudioStream = () => {
        if (
            mediaRecorderRef.current &&
            mediaRecorderRef.current.state === 'recording'
        ) {
            mediaRecorderRef.current.stop();
            console.log('Audio recording stopped');
            setIsRecording(false);
        }

        if (audioStream) {
            audioStream.getTracks().forEach((track) => track.stop());
            setAudioStream(null);
        }
    };

    return (
        <div className="flex flex-col gap-dr-5">
            <h1>Audio Recorder</h1>
            <Button onClick={startAudioStream} disabled={isRecording}>
                Start Audio Stream
            </Button>

            <Button onClick={stopAudioStream} disabled={!isRecording}>
                Stop Audio Stream
            </Button>
        </div>
    );
}

export default Recorder;
