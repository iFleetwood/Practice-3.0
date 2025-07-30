package cc.kasumi.practice.game.queue;

import cc.kasumi.practice.game.ladder.Ladder;

import java.util.ArrayList;
import java.util.List;

public class PartyQueue extends Queue {

    private final int partySize;
    private static final int MAX_PARTIES = 50; // Reasonable limit

    public PartyQueue(String name, Ladder ladder, boolean ranked, int partySize) {
        super(name, QueueType.PARTY, ladder, ranked);
        this.partySize = partySize;
    }

    @Override
    public boolean isReadyForMatch() {
        return getPartyCount() >= 2; // Need at least 2 parties for a match
    }

    @Override
    public int getMinPlayers() {
        return partySize * 2; // 2 parties minimum
    }

    @Override
    public int getMaxPlayers() {
        return partySize * MAX_PARTIES;
    }

    public int getPartyCount() {
        return getPlayerCount() / partySize;
    }

    @Override
    public List<QueuePlayer> getPlayersForMatch() {
        List<QueuePlayer> playersForMatch = new ArrayList<>();

        if (!isReadyForMatch()) {
            return playersForMatch;
        }

        // Take players for 2 parties (minimum for a team match)
        int playersToTake = partySize * 2;

        for (int i = 0; i < playersToTake && i < getPlayerCount(); i++) {
            playersForMatch.add(players.get(i));
        }

        // Remove these players from the queue
        for (QueuePlayer player : playersForMatch) {
            players.remove(player);
        }

        return playersForMatch;
    }

    @Override
    public String getQueueStatus() {
        return String.format("%d parties (%d players)", getPartyCount(), getPlayerCount());
    }

    @Override
    public String getReadyStatusMessage() {
        int partiesNeeded = 2 - getPartyCount();
        if (partiesNeeded <= 0) {
            return "§aReady to start!";
        } else {
            return String.format("§eWaiting for %d more part%s...", partiesNeeded, partiesNeeded == 1 ? "y" : "ies");
        }
    }

    public int getPartySize() {
        return partySize;
    }
}