import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import javax.sound.midi.MidiUnavailableException;
import java.util.Arrays;

public class SerialPortDataListenerIMPL implements SerialPortDataListener
{
    Logger log = LogManager.getLogger(this.getClass());

    MidiEmulator midiEmulator = new MidiEmulator();

    byte lastCommand = 0;

    public SerialPortDataListenerIMPL() throws MidiUnavailableException
    {

    }

    @Override
    public int getListeningEvents()
    {
        return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
    }

    @Override
    public void serialEvent(SerialPortEvent event)
    {
        byte[] data = new byte[event.getSerialPort().bytesAvailable()];
        int numRead = event.getSerialPort().readBytes(data, data.length);

        if (numRead > 0)
        {
            processMidiInputData(data);

                    log.debug(Arrays.toString(data));
        } else
            log.error("Error during data reading!");
    }

    private void onMIDIDataWithStatusByteLead(byte[] data)
    {
        if ((data[0] & 0xF0) == 0b10010000)
        {
            midiEmulator.noteOn(data[0] & 0x0F, data[1], data[2]);

            lastCommand = data[0];

            log.log(Level.toLevel(Priority.INFO_INT),"Note " + data[1] + " on");
        } else if ((data[0] & 0xF0) == 0b10000000)
        {
            midiEmulator.noteOff(data[0] & 0x0F, data[1], data[0]);

            lastCommand = data[0];

            log.log(Level.toLevel(Priority.INFO_INT),"Note " + data[1] + " off");
        } else if ((data[0] & 0xF0) == 0b11100000)
        {
            midiEmulator.sendPitch(data[0] & 0x0F, data[1], data[2]);

            lastCommand = data[0];

            log.log(Level.toLevel(Priority.INFO_INT),"Pitch set to: " + data[1]);
        } else if ((data[0] & 0xF0) == 0b10110000)
        {
            if (data[1] == 7)
            {
                midiEmulator.sendVolumeLevel(data[0] & 0x0F, data[2]);

                lastCommand = data[0];

                log.log(Level.toLevel(Priority.INFO_INT),"Volume level set to: " + data[2]);
            }
        }
    }

    private void onMIDIDataWithDataByteLead(byte[] data)
    {
        if ((lastCommand & 0xF0) == 0b10010000)
        {
            midiEmulator.noteOn(lastCommand & 0x0F, data[0], data[1]);

            log.log(Level.toLevel(Priority.INFO_INT),"Note " + data[0] + " on");
        } else if ((lastCommand & 0xF0) == 0b10000000)
        {
            midiEmulator.noteOff(lastCommand & 0x0F, data[0], data[1]);

            log.log(Level.toLevel(Priority.INFO_INT),"Note " + data[0] + " off");
        } else if ((lastCommand & 0xF0) == 0b11100000)
        {
            midiEmulator.sendPitch(lastCommand & 0x0F, data[0], data[1]);

            log.log(Level.toLevel(Priority.INFO_INT),"Pitch set to: " + (long) (data[0] | (data[1] << 7)));
        } else if ((lastCommand & 0xF0) == 0b10110000)
        {
            if (data[0] == 7)
            {
                midiEmulator.sendVolumeLevel(lastCommand & 0x0F, data[1]);

                log.log(Level.toLevel(Priority.INFO_INT),"Volume level set to: " + data[1]);
            }
        }
    };

    private void processMidiInputData(byte[] data)
    {
        if ((data[0] & 0b10000000) != 0)
        {
            onMIDIDataWithStatusByteLead(data);
        } else
        {
            onMIDIDataWithDataByteLead(data);
        }
    };
}
