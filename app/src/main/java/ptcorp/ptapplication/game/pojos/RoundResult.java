package ptcorp.ptapplication.game.pojos;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.io.Serializable;

/**
 * Created by LinusHakansson on 2018-03-01.
 */

public class RoundResult implements Parcelable, Serializable {
    private static final String TAG = "RoundResult";
    private int hostPoints, clientPoints;
    private final static int GAME_POINTS = 7;
    private RoundStatus roundStatus;
    private boolean isGameOver;
    private RoundLostReason roundLostReason;

    public enum RoundLostReason {
        SHOT_OUT_OF_BOUNDS,
        MISSED_BALL,
        TOOK_TOO_LONG
    }

    public RoundResult() {
        hostPoints = 6;
        clientPoints = 0;
    }

    private enum RoundStatus {
        HOSTWON,
        CLIENTWON
    }

    public void setGameOver(boolean gameOver) {
        this.isGameOver = gameOver;
    }

    public void setHostPoints() {
        this.hostPoints++;
        if (hostPoints==GAME_POINTS)
            isGameOver = true;
    }

    public void setClientPoints() {
        this.clientPoints++;
        if (clientPoints==GAME_POINTS){
            isGameOver = true;
        }
    }

    public RoundLostReason getRoundLostReason() {
        return roundLostReason;
    }

    public void setRoundLostReason(RoundLostReason roundLostReason) {
        this.roundLostReason = roundLostReason;
    }

    public void setRoundStatus(RoundStatus roundStatus) {
        this.roundStatus = roundStatus;
    }

    public int getHostPoints() {
        return hostPoints;
    }

    public int getClientPoints() {
        return clientPoints;
    }
    public boolean isGameOver() {
        return this.isGameOver;
    }

    public RoundStatus getRoundStatus() {
        return roundStatus;
    }

    protected RoundResult(Parcel in) {
        this.hostPoints = in.readInt();
        this.clientPoints = in.readInt();
        String roundStats = in.readString(); // enum parceled as String
        this.roundStatus = RoundStatus.valueOf(roundStats);
        this.isGameOver = in.readByte() != 0x00; // boolean...
        String roundLostReason = in.readString();
        this.roundLostReason = RoundLostReason.valueOf(roundLostReason);

        Log.d(TAG, "writeToParcel: host: " + hostPoints + " client: " + clientPoints);
    }

    public static final Creator<RoundResult> CREATOR = new Creator<RoundResult>() {
        @Override
        public RoundResult createFromParcel(Parcel in) {
            return new RoundResult(in);
        }

        @Override
        public RoundResult[] newArray(int size) {
            return new RoundResult[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(hostPoints);
        dest.writeInt(clientPoints);
        dest.writeString(roundStatus.name()); // send enum as string
        dest.writeByte((byte) (isGameOver ? 0x01 : 0x00)); // true or false
        dest.writeString(roundLostReason.name());

        Log.d(TAG, "writeToParcel: host: " + hostPoints + " client: " + clientPoints);
    }
}
