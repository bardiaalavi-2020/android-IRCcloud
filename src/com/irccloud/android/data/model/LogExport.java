/*
 * Copyright (c) 2016 IRCCloud, Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.irccloud.android.data.model;

import android.app.DownloadManager;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.text.format.DateUtils;

import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.irccloud.android.BR;
import com.irccloud.android.IRCCloudApplication;
import com.irccloud.android.NetworkConnection;
import com.irccloud.android.data.IRCCloudDatabase;
import com.irccloud.android.data.collection.BuffersList;
import com.irccloud.android.data.collection.ServersList;

import java.io.File;

@Entity(indices = {@Index("id"), @Index("download_id")})
public class LogExport extends BaseObservable {
    @PrimaryKey
    private int id;

    private int cid;

    private int bid;

    private String file_name;

    private String redirect_url;

    private long start_date;

    private long finish_date;

    private long expiry_date;

    private long download_id;

    private String name;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCid() {
        return cid;
    }

    public void setCid(int cid) {
        this.cid = cid;
    }

    public int getBid() {
        return bid;
    }

    public void setBid(int bid) {
        this.bid = bid;
    }

    public String getFile_name() {
        return file_name;
    }

    public void setFile_name(String file_name) {
        this.file_name = file_name;
    }

    public String getRedirect_url() {
        return redirect_url;
    }

    public void setRedirect_url(String redirect_url) {
        this.redirect_url = redirect_url;
    }

    public long getStart_date() {
        return start_date;
    }

    public void setStart_date(long start_date) {
        this.start_date = start_date;
    }

    public long getFinish_date() {
        return finish_date;
    }

    public void setFinish_date(long finish_date) {
        this.finish_date = finish_date;
    }

    public long getExpiry_date() {
        return expiry_date;
    }

    public void setExpiry_date(long expiry_date) {
        this.expiry_date = expiry_date;
    }

    public long getDownload_id() {
        return download_id;
    }

    public void setDownload_id(long download_id) {
        this.download_id = download_id;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public void setExpiryTime(String expiryTime) {
        this.expiryTime = expiryTime;
    }

    public boolean isDownloadComplete() {
        return downloadComplete;
    }

    public void setDownloadComplete(boolean downloadComplete) {
        this.downloadComplete = downloadComplete;
    }

    public String getFilesize() {
        return filesize;
    }

    public void setFilesize(String filesize) {
        this.filesize = filesize;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    @Bindable
    public String getName() {
        if(name == null) {
            Buffer b = BuffersList.getInstance().getBuffer(bid);
            Server s = ServersList.getInstance().getServer(cid);

            String serverName = (s != null) ? (s.getName() != null ? s.getName() : s.getHostname()) : "Unknown Network (" + cid + ")";
            String bufferName = (b != null) ? b.getName() : "Unknown Log (" + bid + ")";

            if(bid > 0) {
                if(b != null)
                    name = serverName + ": " + bufferName;
                else
                    return serverName + ": " + bufferName;
            } else if(cid > 0) {
                if(s != null)
                    name = serverName;
                else
                    return serverName;
            } else {
                name = "All Networks";
            }
            if(name != null)
                IRCCloudDatabase.getInstance().LogExportsDao().update(this);
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String startTime;
    @Bindable
    public String getStartTime() {
        if(startTime == null)
            startTime = (finish_date == 0 ? "Started " : "Exported ") + DateUtils.getRelativeTimeSpanString(start_date * 1000L, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS).toString().toLowerCase();
        return startTime;
    }

    private String expiryTime;
    @Bindable
    public String getExpiryTime() {
        if(expiryTime == null)
            expiryTime = "Expires " + DateUtils.getRelativeTimeSpanString(expiry_date * 1000L, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS).toString().toLowerCase();
        return expiryTime;
    }

    private boolean downloadComplete;

    @Bindable
    public int getDownloadProgress() {
        if(download_id > 0 && !downloadComplete) {
            DownloadManager d = (DownloadManager) IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);

            final Cursor downloadCursor = d.query(new DownloadManager.Query().setFilterById(download_id));
            if(downloadCursor != null)
                downloadCursor.registerContentObserver(new ContentObserver(null) {
                    @Override
                    public void onChange(boolean selfChange) {
                        super.onChange(selfChange);
                        notifyPropertyChanged(BR.downloadProgress);
                        notifyPropertyChanged(BR.isDownloading);
                        downloadCursor.unregisterContentObserver(this);
                    }
                });
            if(downloadCursor != null && downloadCursor.moveToFirst()) {
                int status = downloadCursor.getInt(downloadCursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                if (status != DownloadManager.STATUS_FAILED && status != DownloadManager.STATUS_SUCCESSFUL) {
                    int downloaded = downloadCursor.getInt(downloadCursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    int total = downloadCursor.getInt(downloadCursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    return (int)(downloaded * 100.0f / total);
                } else  {
                    downloadComplete = true;
                    if (status == DownloadManager.STATUS_FAILED) {
                        download_id = 0;
                        IRCCloudDatabase.getInstance().LogExportsDao().update(this);
                    }
                }
            } else {
                download_id = 0;
                IRCCloudDatabase.getInstance().LogExportsDao().update(this);
            }
        }
        return -1;
    }

    @Bindable
    public boolean getIsDownloading() {
        return getDownloadProgress() >= 0;
    }

    @Bindable
    public boolean getIsPreparing() {
        return finish_date == 0;
    }

    private String filesize;
    @Bindable
    public String getFileSize() {
        if(filesize == null) {
            if (getExists()) {
                long total = file().length();
                if (total < 1024) {
                    filesize = total + " B";
                } else {
                    int exp = (int) (Math.log(total) / Math.log(1024));
                    filesize = String.format("%.1f ", total / Math.pow(1024, exp)) + ("KMGTPE".charAt(exp - 1)) + "B";
                }
            }
        }
        return filesize;
    }

    public void download() {
        if(redirect_url != null) {
            path().mkdirs();
            DownloadManager d = (DownloadManager) IRCCloudApplication.getInstance().getApplicationContext().getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Request r = new DownloadManager.Request(Uri.parse(redirect_url));
            r.addRequestHeader("Cookie", "session=" + NetworkConnection.getInstance().session);
            r.setDestinationUri(Uri.fromFile(file()));
            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            r.setVisibleInDownloadsUi(true);
            r.setMimeType("application/zip");
            download_id = d.enqueue(r);
            notifyPropertyChanged(BR.downloadProgress);
            notifyPropertyChanged(BR.isDownloading);
            IRCCloudDatabase.getInstance().LogExportsDao().update(this);
        }
    }

    public File path() {
        return new File(IRCCloudApplication.getInstance().getApplicationContext().getExternalFilesDir(null), "export");
    }

    @Ignore
    private File file;
    public File file() {
        if(file == null && file_name != null)
            file = new File(path(), file_name);
        return file;
    }

    @Bindable
    public boolean getExists() {
        return file() != null && file().exists();
    }

    public String toString() {
        return "{id: " + id + ", cid: " + cid + ", bid: " + bid + ", file_name: " + file_name + ", name: " + getName() + "}";
    }

    public void clearCache() {
        expiryTime = null;
        getExpiryTime();
        notifyPropertyChanged(BR.expiryTime);
        startTime = null;
        getStartTime();
        notifyPropertyChanged(BR.startTime);
        filesize = null;
        getFileSize();
        notifyPropertyChanged(BR.fileSize);
    }
}