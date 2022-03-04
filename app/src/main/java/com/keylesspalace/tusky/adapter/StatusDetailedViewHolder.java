package com.keylesspalace.tusky.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.keylesspalace.tusky.R;
import com.keylesspalace.tusky.ViewThreadActivity;
import com.keylesspalace.tusky.entity.Status;
import com.keylesspalace.tusky.interfaces.StatusActionListener;
import com.keylesspalace.tusky.util.CardViewMode;
import com.keylesspalace.tusky.util.LinkHelper;
import com.keylesspalace.tusky.util.StatusDisplayOptions;
import com.keylesspalace.tusky.viewdata.StatusViewData;

import java.text.DateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class StatusDetailedViewHolder extends StatusBaseViewHolder {
    private TextView reblogs;
    private TextView favourites;
    private View infoDivider;

    StatusDetailedViewHolder(View view) {
        super(view);
        reblogs = view.findViewById(R.id.status_reblogs);
        favourites = view.findViewById(R.id.status_favourites);
        infoDivider = view.findViewById(R.id.status_info_divider);
    }

    @Override
    protected int getMediaPreviewHeight(Context context) {
        return context.getResources().getDimensionPixelSize(R.dimen.status_detail_media_preview_height);
    }

    @Override
    protected void setCreatedAt(Date createdAt, StatusDisplayOptions statusDisplayOptions) {
        if (createdAt == null) {
            timestampInfo.setText("");
        } else {
            DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT);
            timestampInfo.setText(dateFormat.format(createdAt));
        }
    }

    private void setReblogAndFavCount(int reblogCount, int favCount, StatusActionListener listener) {

        if (reblogCount > 0) {
            reblogs.setText(getReblogsText(reblogs.getContext(), reblogCount));
            reblogs.setVisibility(View.VISIBLE);
        } else {
            reblogs.setVisibility(View.GONE);
        }
        if (favCount > 0) {
            favourites.setText(getFavsText(favourites.getContext(), favCount));
            favourites.setVisibility(View.VISIBLE);
        } else {
            favourites.setVisibility(View.GONE);
        }

        if (reblogs.getVisibility() == View.GONE && favourites.getVisibility() == View.GONE) {
            infoDivider.setVisibility(View.GONE);
        } else {
            infoDivider.setVisibility(View.VISIBLE);
        }

        reblogs.setOnClickListener(v -> {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onShowReblogs(position);
            }
        });
        favourites.setOnClickListener(v -> {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onShowFavs(position);
            }
        });
    }

    private void setApplication(@Nullable Status.Application app) {
        if (app != null) {

            timestampInfo.append("  •  ");

            if (app.getWebsite() != null) {
                CharSequence text = LinkHelper.createClickableText(app.getName(), app.getWebsite());
                timestampInfo.append(text);
                timestampInfo.setMovementMethod(LinkMovementMethod.getInstance());
            } else {
                timestampInfo.append(app.getName());
            }
        }
    }

    @Override
    public void setupWithStatus(final StatusViewData.Concrete status,
                                   final StatusActionListener listener,
                                   StatusDisplayOptions statusDisplayOptions,
                                   @Nullable Object payloads) {
        super.setupWithStatus(status, listener, statusDisplayOptions, payloads);
        setupCard(status, CardViewMode.FULL_WIDTH, statusDisplayOptions, listener); // Always show card for detailed status
        if (payloads == null) {

            if (!statusDisplayOptions.hideStats()) {
                setReblogAndFavCount(status.getActionable().getReblogsCount(),
                        status.getActionable().getFavouritesCount(), listener);
            } else {
                hideQuantitativeStats();
            }

            setApplication(status.getActionable().getApplication());

            View.OnLongClickListener longClickListener = view -> {
                TextView textView = (TextView) view;
                ClipboardManager clipboard = (ClipboardManager) view.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("toot", textView.getText());
                clipboard.setPrimaryClip(clip);

                Toast.makeText(view.getContext(), R.string.copy_to_clipboard_success, Toast.LENGTH_SHORT).show();

                return true;
            };

            content.setOnLongClickListener(longClickListener);
            contentWarningDescription.setOnLongClickListener(longClickListener);
        }
    }

    private void hideQuantitativeStats() {
        reblogs.setVisibility(View.GONE);
        favourites.setVisibility(View.GONE);
        infoDivider.setVisibility(View.GONE);
    }
}
