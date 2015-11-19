package com.faforever.client.chat;

import com.faforever.client.legacy.GameStatus;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.PlayerInfo;
import com.faforever.client.legacy.domain.Player;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableSet;

/**
 * Represents a player with username, clan, country, friend/foe flag and so on. Can also be a chat-only user. This
 * represents the combination of a PlayerInfo (from the FAF server) and a ChatUser (from IRC).
 */
public class PlayerInfoBean {

  private final StringProperty username;
  private final StringProperty clan;
  private final StringProperty country;
  private final StringProperty avatarUrl;
  private final StringProperty avatarTooltip;
  private final BooleanProperty friend;
  private final BooleanProperty foe;
  private final SetProperty<String> moderatorForChannels;
  private final BooleanProperty chatOnly;
  private final FloatProperty globalRatingDeviation;
  private final FloatProperty globalRatingMean;
  private final FloatProperty leaderboardRatingDeviation;
  private final FloatProperty leaderboardRatingMean;
  private final IntegerProperty gameUid;
  private final SimpleObjectProperty<GameStatus> gameStatus;
  private final IntegerProperty numberOfGames;

  public PlayerInfoBean(Player player) {
    this();

    username.set(player.getLogin());
    clan.set(player.getClan());
    country.set(player.getCountry());

    if (player.getAvatar() != null) {
      avatarTooltip.set(player.getAvatar().getTooltip());
      avatarUrl.set(player.getAvatar().getUrl());
    }
  }

  private PlayerInfoBean() {
    username = new SimpleStringProperty();
    clan = new SimpleStringProperty();
    country = new SimpleStringProperty();
    avatarUrl = new SimpleStringProperty();
    avatarTooltip = new SimpleStringProperty();
    friend = new SimpleBooleanProperty();
    foe = new SimpleBooleanProperty();
    moderatorForChannels = new SimpleSetProperty<>();
    chatOnly = new SimpleBooleanProperty(true);
    globalRatingDeviation = new SimpleFloatProperty();
    globalRatingMean = new SimpleFloatProperty();
    leaderboardRatingDeviation = new SimpleFloatProperty();
    leaderboardRatingMean = new SimpleFloatProperty();
    gameStatus = new SimpleObjectProperty<>();
    gameUid = new SimpleIntegerProperty();
    numberOfGames = new SimpleIntegerProperty();
  }

  public PlayerInfoBean(String username) {
    this();
    this.gameStatus.set(GameStatus.NONE);
    this.username.set(username);
  }

  public int getNumberOfGames() {
    return numberOfGames.get();
  }

  public void setNumberOfGames(int numberOfGames) {
    this.numberOfGames.set(numberOfGames);
  }

  public IntegerProperty numberOfGamesProperty() {
    return numberOfGames;
  }

  @Override
  public int hashCode() {
    return username.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    return (obj.getClass() == PlayerInfoBean.class)
        && getUsername().equals(((PlayerInfoBean) obj).getUsername());
  }

  public String getUsername() {
    return username.get();
  }

  public void setUsername(String username) {
    this.username.set(username);
  }

  public StringProperty usernameProperty() {
    return username;
  }

  public String getClan() {
    return clan.get();
  }

  public void setClan(String clan) {
    this.clan.set(clan);
  }

  public StringProperty clanProperty() {
    return clan;
  }

  public String getCountry() {
    return country.get();
  }

  public void setCountry(String country) {
    this.country.set(country);
  }

  public StringProperty countryProperty() {
    return country;
  }

  public String getAvatarUrl() {
    return avatarUrl.get();
  }

  public void setAvatarUrl(String avatarUrl) {
    this.avatarUrl.set(avatarUrl);
  }

  public StringProperty avatarUrlProperty() {
    return avatarUrl;
  }

  public String getAvatarTooltip() {
    return avatarTooltip.get();
  }

  public void setAvatarTooltip(String avatarTooltip) {
    this.avatarTooltip.set(avatarTooltip);
  }

  public StringProperty avatarTooltipProperty() {
    return avatarTooltip;
  }

  public boolean isFriend() {
    return friend.get();
  }

  public BooleanProperty friendProperty() {
    return friend;
  }

  public boolean isFoe() {
    return foe.get();
  }

  public BooleanProperty foeProperty() {
    return foe;
  }

  public boolean isChatOnly() {
    return chatOnly.get();
  }

  public BooleanProperty chatOnlyProperty() {
    return chatOnly;
  }

  public boolean getFriend() {
    return friend.get();
  }

  public void setFriend(boolean friend) {
    this.friend.set(friend);
  }

  public boolean getFoe() {
    return foe.get();
  }

  public void setFoe(boolean foe) {
    this.foe.set(foe);
  }

  public ObservableSet<String> getModeratorForChannels() {
    return moderatorForChannels.get();
  }

  public SetProperty<String> moderatorForChannelsProperty() {
    return moderatorForChannels;
  }

  public boolean getChatOnly() {
    return chatOnly.get();
  }

  public void setChatOnly(boolean chatOnly) {
    this.chatOnly.set(chatOnly);
  }

  public float getGlobalRatingDeviation() {
    return globalRatingDeviation.get();
  }

  public void setGlobalRatingDeviation(float globalRatingDeviation) {
    this.globalRatingDeviation.set(globalRatingDeviation);
  }

  public FloatProperty globalRatingDeviationProperty() {
    return globalRatingDeviation;
  }

  public float getGlobalRatingMean() {
    return globalRatingMean.get();
  }

  public void setGlobalRatingMean(float globalRatingMean) {
    this.globalRatingMean.set(globalRatingMean);
  }

  public FloatProperty globalRatingMeanProperty() {
    return globalRatingMean;
  }

  public GameStatus getGameStatus() {
    return gameStatus.get();
  }

  public void setGameStatus(GameStatus gameStatus) {
    this.gameStatus.set(gameStatus);
  }

  public SimpleObjectProperty<GameStatus> gameStatusProperty() {
    return gameStatus;
  }

  public void setGameStatusFromGameState(GameState gameState){
    gameStatus.set(GameStatus.getFromGameState(gameState));
  }

  public int getGameUid() {
    return gameUid.get();
  }

  public void setGameUid(int gameUid) {
    this.gameUid.set(gameUid);
  }

  public IntegerProperty gameUidProperty() {
    return gameUid;
  }

  public float getLeaderboardRatingMean() {
    return leaderboardRatingMean.get();
  }

  public void setLeaderboardRatingMean(float leaderboardRatingMean) {
    this.leaderboardRatingMean.set(leaderboardRatingMean);
  }

  public FloatProperty leaderboardRatingMeanProperty() {
    return leaderboardRatingMean;
  }

  public float getLeaderboardRatingDeviation() {
    return leaderboardRatingDeviation.get();
  }

  public void setLeaderboardRatingDeviation(float leaderboardRatingDeviation) {
    this.leaderboardRatingDeviation.set(leaderboardRatingDeviation);
  }

  public FloatProperty leaderboardRatingDeviationProperty() {
    return leaderboardRatingDeviation;
  }

  public void updateFromPlayerInfo(Player player) {
    setChatOnly(false);
    setClan(player.getClan());
    setCountry(player.getCountry());
    setGlobalRatingMean(player.getRatingMean());
    setGlobalRatingDeviation(player.getRatingDeviation());
    setLeaderboardRatingDeviation(player.getLadderRatingDeviation());
    setLeaderboardRatingMean(player.getLadderRatingMean());
    setNumberOfGames(player.getNumberOfGames());
    if (player.getAvatar() != null) {
      setAvatarUrl(player.getAvatar().getUrl());
      setAvatarTooltip(player.getAvatar().getTooltip());
    }
  }
}
