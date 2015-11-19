package com.faforever.client.player;

import com.faforever.client.chat.PlayerInfoBean;
import com.faforever.client.game.GameInfoBean;
import com.faforever.client.game.GameService;
import com.faforever.client.legacy.GameStatus;
import com.faforever.client.legacy.LobbyServerAccessor;
import com.faforever.client.legacy.OnFoeListListener;
import com.faforever.client.legacy.OnFriendListListener;
import com.faforever.client.legacy.OnPlayerInfoListener;
import com.faforever.client.legacy.domain.GameState;
import com.faforever.client.legacy.domain.PlayerInfo;
import com.faforever.client.legacy.domain.Player;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Assert;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PlayerServiceImpl implements PlayerService, OnPlayerInfoListener, OnFoeListListener, OnFriendListListener {

  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private static final Lock CURRENT_PLAYER_LOCK = new ReentrantLock();

  private final ObservableMap<String, PlayerInfoBean> players;

  @Resource
  LobbyServerAccessor lobbyServerAccessor;
  @Resource
  UserService userService;

  @Autowired
  GameService gameService;

  private List<String> foeList;
  private List<String> friendList;
  private ObjectProperty<PlayerInfoBean> currentPlayer;
  private GameStatus gameStatus;

  public PlayerServiceImpl() {
    players = FXCollections.observableHashMap();
    friendList = new ArrayList<>();
    foeList = new ArrayList<>();
    gameStatus = GameStatus.NONE;
    currentPlayer = new SimpleObjectProperty<>();
  }

  public GameStatus getGameStatus() {
    return gameStatus;
  }

  public void setGameStatus(GameStatus gameStatus) {
    this.gameStatus = gameStatus;
  }

  @PostConstruct
  void init() {
    lobbyServerAccessor.setOnPlayerInfoMessageListener(this);
    lobbyServerAccessor.setOnFoeListListener(this);
    lobbyServerAccessor.setOnFriendListListener(this);
    gameService.addOnGameInfoBeanListener(change -> {
      while (change.next()) {
        for (GameInfoBean gameInfoBean : change.getRemoved()) {
          gameInfoBean.setStatus(GameState.CLOSED);
          gameInfoBean.getTeams().forEach((team, players) -> updatePlayerInfoBean(players, gameInfoBean));
        }
        for (GameInfoBean gameInfoBean : change.getAddedSubList()) {
          gameInfoBean.getTeams().forEach((team, players) -> updatePlayerInfoBean(players, gameInfoBean));
          gameInfoBean.statusProperty().addListener(change2 -> {
            gameInfoBean.getTeams().forEach((team, updatedPlayer) -> updatePlayerInfoBean(updatedPlayer, gameInfoBean));
          });
        }
      }
    });
  }

  //FIXME ugly fix until host can be resolved from gamestate
  private void updatePlayerInfoBean(List<String> players, GameInfoBean gameInfoBean) {
    for (String player : players) {
      PlayerInfoBean playerInfoBean = getPlayerForUsername(player);
      if(playerInfoBean == null) {
        continue;
      }
      updatePlayerGameStatus(playerInfoBean, GameStatus.getFromGameState(gameInfoBean.getStatus()));
      playerInfoBean.setGameUid(gameInfoBean.getUid());
    }
    if (GameStatus.getFromGameState(gameInfoBean.getStatus()) == GameStatus.LOBBY) {
      PlayerInfoBean host = getPlayerForUsername(gameInfoBean.getHost());
      updatePlayerGameStatus(host, GameStatus.HOST);
    }
  }

  @Override
  public void updatePlayerGameStatus(PlayerInfoBean playerInfoBean, GameStatus gameStatus) {
    if (playerInfoBean != null && playerInfoBean.getGameStatus() != gameStatus) {
      //FIXME until api, host is set twice or ugly code, I chose to set twice
      playerInfoBean.setGameStatus(gameStatus);
    }
  }

  @Override
  public PlayerInfoBean getPlayerForUsername(String username) {
    PlayerInfoBean playerInfoBean = players.get(username);
    if (playerInfoBean == null) {
      logger.warn("Unknown user: {}", username);
    }
    return playerInfoBean;
  }

  @Override
  public PlayerInfoBean registerAndGetPlayerForUsername(@NotNull String username) {
    Assert.checkNull(username, "username must not be null");

    if (!players.containsKey(username)) {
      players.put(username, new PlayerInfoBean(username));
    }

    return players.get(username);
  }

  @Override
  public Set<String> getPlayerNames() {
    return players.keySet();
  }

  @Override
  public void addFriend(String username) {
    players.get(username).setFriend(true);
    players.get(username).setFoe(false);
    friendList.add(username);
    foeList.remove(username);

    lobbyServerAccessor.setFriends(friendList);
  }

  @Override
  public void removeFriend(String username) {
    players.get(username).setFriend(false);
    friendList.remove(username);

    lobbyServerAccessor.setFriends(friendList);
  }

  @Override
  public void addFoe(String username) {
    players.get(username).setFoe(true);
    players.get(username).setFriend(false);
    foeList.add(username);
    friendList.remove(username);

    lobbyServerAccessor.setFoes(foeList);
  }

  @Override
  public void removeFoe(String username) {
    players.get(username).setFoe(false);
    foeList.remove(username);

    lobbyServerAccessor.setFoes(foeList);
  }

  @Override
  public PlayerInfoBean getCurrentPlayer() {
    CURRENT_PLAYER_LOCK.lock();
    try {
      if (currentPlayer.get() == null) {
        currentPlayer.set(registerAndGetPlayerForUsername(userService.getUsername()));
      }
    } finally {
      CURRENT_PLAYER_LOCK.unlock();
    }
    return currentPlayer.get();
  }

  @Override
  public ReadOnlyObjectProperty<PlayerInfoBean> currentPlayerProperty() {
    return currentPlayer;
  }


  @Override
  public void onPlayerInfo(Player player) {
    if (player.getLogin().equals(userService.getUsername())) {
      PlayerInfoBean playerInfoBean = getCurrentPlayer();
      playerInfoBean.updateFromPlayerInfo(player);
    } else {
      PlayerInfoBean playerInfoBean = registerAndGetPlayerForUsername(player.getLogin());
      playerInfoBean.setFriend(friendList.contains(player.getLogin()));
      playerInfoBean.setFoe(foeList.contains(player.getLogin()));
      playerInfoBean.updateFromPlayerInfo(player);
    }
  }

  @Override
  public void onFoeList(List<String> foes) {
    this.foeList = foes;

    for (String foe : foes) {
      PlayerInfoBean playerInfoBean = players.get(foe);
      if (playerInfoBean != null) {
        playerInfoBean.setFoe(true);
      }
    }
  }

  @Override
  public void onFriendList(List<String> friends) {
    this.friendList = friends;

    for (String friend : friendList) {
      PlayerInfoBean playerInfoBean = players.get(friend);
      if (playerInfoBean != null) {
        playerInfoBean.setFriend(true);
      }
    }
  }
}
