package com.ft.emulator.server.game.core.matchplay.basic;

import com.ft.emulator.server.game.core.constants.ServeType;
import com.ft.emulator.server.game.core.matchplay.MatchplayGame;
import com.ft.emulator.server.game.core.matchplay.room.RoomPlayer;
import com.ft.emulator.server.game.core.matchplay.room.ServeInfo;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

@Getter
@Setter
public class MatchplayBasicGame extends MatchplayGame {
    private byte pointsRedTeam;
    private byte pointsBlueTeam;
    private byte setsRedTeam;
    private byte setsBlueTeam;
    private RoomPlayer servePlayer;
    private RoomPlayer receiverPlayer;

    public MatchplayBasicGame() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        this.setStartTime(cal.getTime());

        this.pointsRedTeam = 0;
        this.pointsBlueTeam = 0;
        this.setsRedTeam = 0;
        this.setsBlueTeam = 0;
        this.setFinished(false);
    }

    public void setPoints(byte pointsRedTeam, byte pointsBlueTeam) {
        this.pointsRedTeam = pointsRedTeam;
        this.pointsBlueTeam = pointsBlueTeam;

        if (pointsRedTeam == 4 && pointsBlueTeam < 3) {
            this.setsRedTeam++;
            resetPoints();
        }
        else if (pointsRedTeam > 4 && (pointsRedTeam - pointsBlueTeam) == 2) {
            this.setsRedTeam++;
            resetPoints();
        }
        else if (pointsBlueTeam == 4 && pointsRedTeam < 3) {
            this.setsBlueTeam++;
            resetPoints();
        }
        else if (pointsBlueTeam > 4 && (pointsBlueTeam - pointsRedTeam) == 2) {
            this.setsBlueTeam++;
            resetPoints();
        }

        if (this.setsRedTeam == 2 || this.setsBlueTeam == 2) {
            this.setFinished(true);

            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            this.setEndTime(cal.getTime());
        }
    }

    private void resetPoints() {
        this.pointsRedTeam = 0;
        this.pointsBlueTeam = 0;
    }

    @Override
    public long getTimeNeeded() {
        return getEndTime().getTime() - getStartTime().getTime();
    }

    @Override
    public boolean isRedTeam(int playerPos) {
        return playerPos == 0 || playerPos == 2;
    }

    @Override
    public boolean isBlueTeam(int playerPos) {
        return playerPos == 1 || playerPos == 3;
    }

    public Point invertPointX(Point point) {
        return new Point(point.x * (-1), point.y);
    }

    public Point invertPointY(Point point) {
        return new Point(point.x, point.y  * (-1));
    }

    public boolean shouldSwitchServingSide(boolean isSingles, boolean isRedTeamServing, boolean anyTeamWonSet, int playerPosition) {
        if (anyTeamWonSet) {
            return false;
        }

        if (isSingles) {
            return true;
        }
        return isRedTeamServing && this.isRedTeam(playerPosition) || !isRedTeamServing && this.isBlueTeam(playerPosition);
    }

    public boolean isRedTeamServing(int timesCourtChanged) {
        return this.isEven(timesCourtChanged);
    }

    public boolean shouldPlayerServe(boolean isSingles, int timesCourtChanged, int playerPosition) {
        if (isSingles) {
            if (this.isEven(playerPosition) && this.isEven(timesCourtChanged)) {
                return true;
            }

            return !this.isEven(playerPosition) && !this.isEven(timesCourtChanged);
        } else {
            return playerPosition == timesCourtChanged;
        }
    }

    public void setPlayerLocationsForDoubles(List<ServeInfo> serveInfo) {
        long outerFieldPosY = 125;
        long innerFieldPosY = 75;

        ServeInfo server = serveInfo.stream().filter(x -> x.getServeType() == ServeType.ServeBall).findFirst().orElse(null);
        if (server == null) return;

        Point serverLocation = server.getPlayerStartLocation();
        serverLocation.setLocation(serverLocation.x, this.getCorrectCourtPositionY(serverLocation.y, outerFieldPosY));

        ServeInfo nonServer = serveInfo.stream()
                .filter(x -> this.areInSameTeam(server.getPlayerPosition(), x.getPlayerPosition()) && x.getPlayerStartLocation().x == serverLocation.x * (-1))
                .findFirst()
                .orElse(null);
        if (nonServer != null) {
            Point nonServerStartLocation = nonServer.getPlayerStartLocation();
            nonServer.getPlayerStartLocation().setLocation(nonServerStartLocation.x, this.getCorrectCourtPositionY(nonServerStartLocation.y, innerFieldPosY));
        }

        ServeInfo receiver = serveInfo.stream()
                .filter(x -> !this.areInSameTeam(server.getPlayerPosition(), x.getPlayerPosition()) && x.getPlayerStartLocation().x == serverLocation.x * (-1))
                .findFirst()
                .orElse(null);
        if (receiver != null) {
            Point receiverStartLocation = receiver.getPlayerStartLocation();
            receiver.getPlayerStartLocation().setLocation(receiverStartLocation.x, this.getCorrectCourtPositionY(receiverStartLocation.y, outerFieldPosY));
            receiver.setServeType(ServeType.ReceiveBall);
        }

        ServeInfo nonReceiver = serveInfo.stream()
                .filter(x -> !this.areInSameTeam(server.getPlayerPosition(), x.getPlayerPosition()) && x.getPlayerStartLocation().x == serverLocation.x)
                .findFirst()
                .orElse(null);
        if (nonReceiver != null) {
            Point nonReceiverPlayerStartLocation = nonReceiver.getPlayerStartLocation();
            nonReceiver.getPlayerStartLocation().setLocation(nonReceiverPlayerStartLocation.x, this.getCorrectCourtPositionY(nonReceiverPlayerStartLocation.y, innerFieldPosY));
        }
    }

    private boolean areInSameTeam(int playerPosition, int playerPosition1) {
        return this.isRedTeam(playerPosition) && this.isRedTeam(playerPosition1) ||
                this.isBlueTeam(playerPosition) && this.isBlueTeam(playerPosition1);
    }

    private long getCorrectCourtPositionY(long playerPositionY, long targetYPosition) {
        return playerPositionY > 0 ? targetYPosition : -targetYPosition;
    }

    private boolean isEven(int number) {
        return number % 2 == 0;
    }
}