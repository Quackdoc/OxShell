package com.OxGames.OxShell.Helpers;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import com.OxGames.OxShell.Data.DataLocation;
import com.OxGames.OxShell.Data.DataRef;
import com.OxGames.OxShell.OxShellApp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class AudioPool {
    private DataRef dataRef;
    private final List<MediaPlayer> beingPrepped;
    private final Queue<MediaPlayer> unusedPlayers;
    private final LinkedList<MediaPlayer> playingPlayers;

    private AudioPool() {
        beingPrepped = new ArrayList<>();
        unusedPlayers = new ArrayDeque<>();
        playingPlayers = new LinkedList<>();
    }
    public boolean isPlayerAvailable() {
        return !unusedPlayers.isEmpty();
    }

    public void play(boolean loop) {
        if (isPlayerAvailable()) {
            MediaPlayer player = unusedPlayers.poll();
            playingPlayers.add(player);
            player.setOnCompletionListener(mp -> {
                if (playingPlayers.contains(mp)) {
                    playingPlayers.remove(mp);
                    unusedPlayers.add(mp);
                }
            });
            player.setLooping(loop);
            player.start();
        }
    }
    public void stopAll() {
        for (MediaPlayer mp : playingPlayers) {
            mp.stop();
            playingPlayers.remove(mp);
            unusedPlayers.add(mp);
        }
    }

    public int getPoolSize() {
        return beingPrepped.size() + unusedPlayers.size() + playingPlayers.size();
    }
    public void setPoolSize(int size) {
        int diff = size - getPoolSize();
        if (diff > 0) {
            // add media players
            new Thread(() -> {
                try {
                    AssetFileDescriptor afd = null;
                    if (dataRef.getLocType() == DataLocation.asset)
                        OxShellApp.getContext().getAssets().openFd((String)dataRef.getLoc());
                    for (int i = 0; i < diff; i++) {
                        MediaPlayer player = new MediaPlayer();
                        try {
                            if (dataRef.getLocType() == DataLocation.asset)
                                player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                            else if (dataRef.getLocType() == DataLocation.file)
                                player.setDataSource((String)dataRef.getLoc());
                            else if (dataRef.getLocType() == DataLocation.resolverUri)
                                player.setDataSource(OxShellApp.getContext(), (Uri)dataRef.getLoc());
                            else
                                throw new UnsupportedOperationException("Cannot load data type: " + dataRef.getLocType());
                            beingPrepped.add(player);
                            new Thread(() -> {
                                try {
                                    player.prepare();
                                    if (beingPrepped.contains(player)) {
                                        // if its not in beingPrepped, that means its been 'cancelled'
                                        beingPrepped.remove(player);
                                        unusedPlayers.add(player);
                                    }
                                } catch (Exception e) {
                                    Log.e("AudioHelper", "Failed to prepare MediaPlayer: " + e);
                                }
                            }).start();
                        } catch (Exception e) {
                            Log.e("AudioHelper", "Failed to load asset into MediaPlayer: " + e);
                        }
                    }
                    if (afd != null)
                        afd.close();
                } catch (Exception e) {
                    Log.e("AudioHelper", "Failed to read asset: " + e);
                }
            }).start();
        } else if (diff < 0) {
            // remove media players
            int removeCount = Math.abs(diff);
            int removed = 0;
            if (beingPrepped.size() > 0) {
                for (int i = 0; i < Math.min(beingPrepped.size(), removeCount); i++) {
                    int lastIndex = (beingPrepped.size() - 1) - i;
                    MediaPlayer player = beingPrepped.get(lastIndex);
                    player.reset();
                    player.release();
                    beingPrepped.remove(lastIndex);
                    removed++;
                }
            }
            if (removed < removeCount && unusedPlayers.size() > 0) {
                for (int i = 0; i < Math.min(unusedPlayers.size(), removeCount); i++) {
                    MediaPlayer player = unusedPlayers.poll();
                    player.reset();
                    player.release();
                    removed++;
                }
            }
            if (removed < removeCount && playingPlayers.size() > 0) {
                for (int i = 0; i < Math.min(playingPlayers.size(), removeCount); i++) {
                    MediaPlayer player = playingPlayers.poll();
                    player.reset();
                    player.release();
                    removed++;
                }
            }
        }
    }
    public static AudioPool fromAsset(String assetLoc, int poolSize) {
        AudioPool pool = new AudioPool();
        pool.dataRef = DataRef.from(assetLoc, DataLocation.asset);
        pool.setPoolSize(poolSize);
        return pool;
    }
    public static AudioPool fromFile(String filePath, int poolSize) {
        AudioPool pool = new AudioPool();
        pool.dataRef = DataRef.from(filePath, DataLocation.file);
        pool.setPoolSize(poolSize);
        return pool;
    }
    public static AudioPool fromUri(Uri uri, int poolSize) {
        AudioPool pool = new AudioPool();
        pool.dataRef = DataRef.from(uri, DataLocation.resolverUri);
        pool.setPoolSize(poolSize);
        return pool;
    }
}
