package mega.privacy.android.app.presentation.filecontact;

import static mega.privacy.android.app.utils.AvatarUtil.getColorAvatar;
import static mega.privacy.android.app.utils.AvatarUtil.getDefaultAvatar;
import static mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile;
import static mega.privacy.android.app.utils.ChatUtil.StatusIconLocation;
import static mega.privacy.android.app.utils.ChatUtil.setContactStatus;
import static mega.privacy.android.app.utils.Constants.AVATAR_SIZE;
import static mega.privacy.android.app.utils.ContactUtil.getMegaUserNameDB;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.Util.scaleWidthPx;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;
import timber.log.Timber;

public class MegaSharedFolderAdapter extends RecyclerView.Adapter<MegaSharedFolderAdapter.ViewHolderShareList> implements OnClickListener, View.OnLongClickListener {

    Context context;
    int positionClicked;
    List<MegaShare> shareList;
    MegaNode node;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    boolean multipleSelect = false;

    OnItemClickListener mItemClickListener;
    RecyclerView listFragment;

    AlertDialog permissionsDialog;
    SparseBooleanArray selectedItems;

    final MegaSharedFolderAdapter megaSharedFolderAdapter;

    public static ArrayList<String> pendingAvatars = new ArrayList<String>();

    @Override
    public boolean onLongClick(View v) {

        ViewHolderShareList holder = (ViewHolderShareList) v.getTag();
        int currentPosition = holder.currentPosition;

        if (context instanceof FileContactListActivity) {
            ((FileContactListActivity) context).activateActionMode();
            ((FileContactListActivity) context).itemClick(currentPosition);
        }

        return true;
    }

    private class UserAvatarListenerList implements MegaRequestListenerInterface {

        Context context;
        ViewHolderShareList holder;
        MegaSharedFolderAdapter adapter;

        public UserAvatarListenerList(Context context, ViewHolderShareList holder, MegaSharedFolderAdapter adapter) {
            this.context = context;
            this.holder = holder;
            this.adapter = adapter;
        }

        @Override
        public void onRequestStart(MegaApiJava api, MegaRequest request) {
            Timber.d("onRequestStart() avatar");
        }

        @Override
        public void onRequestFinish(MegaApiJava api, MegaRequest request,
                                    MegaError e) {
            Timber.d("onRequestFinish() avatar");
            if (e.getErrorCode() == MegaError.API_OK) {

                if (request.getEmail() != null) {
                    pendingAvatars.remove(request.getEmail());

                    if (holder.contactMail.compareTo(request.getEmail()) == 0) {
                        File avatar = buildAvatarFile(holder.contactMail + ".jpg");
                        Bitmap bitmap = null;
                        if (isFileAvailable(avatar)) {
                            if (avatar.length() > 0) {
                                BitmapFactory.Options bOpts = new BitmapFactory.Options();
                                bOpts.inPurgeable = true;
                                bOpts.inInputShareable = true;
                                bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                                if (bitmap == null) {
                                    avatar.delete();
                                } else {
                                    holder.imageView.setImageBitmap(bitmap);
                                }
                            }
                        }
                    }
                }
            } else {
                Timber.e("E: %s___%s", e.getErrorCode(), e.getErrorString());
            }
        }

        @Override
        public void onRequestTemporaryError(MegaApiJava api,
                                            MegaRequest request, MegaError e) {
            Timber.w("onRequestTemporaryError");
        }

        @Override
        public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

        }
    }

    public MegaSharedFolderAdapter(Context _context, MegaNode node, List<MegaShare> _shareList, RecyclerView _lv) {
        this.context = _context;
        this.node = node;
        this.shareList = _shareList;
        this.positionClicked = -1;
        this.megaSharedFolderAdapter = this;
        this.listFragment = _lv;

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
        }
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setNode(MegaNode node) {
        this.node = node;
    }

    /*private view holder class*/
    class ViewHolderShareList extends RecyclerView.ViewHolder implements View.OnClickListener {
        RoundedImageView imageView;
        ImageView verifiedIcon;
        EmojiTextView textViewContactName;
        TextView textViewPermissions;
        RelativeLayout threeDotsLayout;
        RelativeLayout itemLayout;
        int currentPosition;
        String contactMail;
        boolean name = false;
        boolean firstName = false;
        ImageView stateIcon;

        public ViewHolderShareList(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mItemClickListener != null) {
                mItemClickListener.onItemClick(v, getPosition());
            }
        }
    }

    public interface OnItemClickListener {
        public void onItemClick(View view, int position);
    }

    public void SetOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }


    @Override
    public ViewHolderShareList onCreateViewHolder(ViewGroup parent, int viewType) {

        listFragment = (RecyclerView) parent;

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        float density = ((Activity) context).getResources().getDisplayMetrics().density;

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shared_folder, parent, false);
        ViewHolderShareList holder = new ViewHolderShareList(v);
        holder.itemLayout = (RelativeLayout) v.findViewById(R.id.shared_folder_item_layout);
        holder.itemLayout.setOnClickListener(this);
        holder.itemLayout.setOnLongClickListener(this);
        holder.imageView = (RoundedImageView) v.findViewById(R.id.shared_folder_contact_thumbnail);
        holder.verifiedIcon = v.findViewById(R.id.verified_icon);

        holder.textViewContactName = v.findViewById(R.id.shared_folder_contact_name);
        holder.textViewContactName.setTypeEllipsize(TextUtils.TruncateAt.MIDDLE);
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            holder.textViewContactName.setMaxWidthEmojis(scaleWidthPx(280, outMetrics));
        } else {
            holder.textViewContactName.setMaxWidthEmojis(scaleWidthPx(225, outMetrics));
        }

        holder.textViewPermissions = (TextView) v.findViewById(R.id.shared_folder_contact_permissions);
        holder.threeDotsLayout = (RelativeLayout) v.findViewById(R.id.shared_folder_three_dots_layout);

        holder.stateIcon = (ImageView) v.findViewById(R.id.shared_folder_state_icon);

        v.setTag(holder);

        return holder;
    }


    @Override
    public void onBindViewHolder(ViewHolderShareList holder, @SuppressLint("RecyclerView") int position) {
        Timber.d("Position: %s", position);

        holder.currentPosition = position;

        //Check if the share
        MegaShare share = (MegaShare) getItem(position);

        if (share.getUser() != null) {
            holder.contactMail = share.getUser();
            MegaUser contact = megaApi.getContact(holder.contactMail);
            holder.verifiedIcon.setVisibility(contact != null && megaApi.areCredentialsVerified(contact) ? View.VISIBLE : View.GONE);

            if (contact != null) {
                holder.textViewContactName.setText(getMegaUserNameDB(contact));

                holder.stateIcon.setVisibility(View.VISIBLE);
                setContactStatus(megaChatApi.getUserOnlineStatus(contact.getHandle()), holder.stateIcon, StatusIconLocation.STANDARD);
            } else {
                holder.textViewContactName.setText(holder.contactMail);
            }

            if (multipleSelect && this.isItemChecked(position)) {
                holder.imageView.setImageResource(R.drawable.ic_chat_avatar_select);
            } else {
                /*Default Avatar*/
                int color = getColorAvatar(contact);
                String name = " ";
                if (holder.textViewContactName != null) {
                    name = holder.textViewContactName.getText().toString();
                } else if (holder.contactMail != null && holder.contactMail.length() > 0) {
                    name = holder.contactMail;
                }
                holder.imageView.setImageBitmap(getDefaultAvatar(color, name, AVATAR_SIZE, true));

                /*Avatar*/
                if (contact != null) {
                    UserAvatarListenerList listener = new UserAvatarListenerList(context, holder, this);
                    holder.name = false;
                    holder.firstName = false;
                    megaApi.getUserAttribute(contact, 1, listener);
                    megaApi.getUserAttribute(contact, 2, listener);

                    File avatar = buildAvatarFile(holder.contactMail + ".jpg");
                    Bitmap bitmap;
                    if (isFileAvailable(avatar) && avatar.length() > 0) {
                        BitmapFactory.Options bOpts = new BitmapFactory.Options();
                        bitmap = BitmapFactory.decodeFile(avatar.getAbsolutePath(), bOpts);
                        if (bitmap == null) {
                            avatar.delete();
                            megaApi.getUserAvatar(contact, buildAvatarFile(holder.contactMail + ".jpg").getAbsolutePath(), listener);
                        } else {
                            holder.imageView.setImageBitmap(bitmap);
                        }
                    } else {
                        megaApi.getUserAvatar(contact, buildAvatarFile(holder.contactMail + ".jpg").getAbsolutePath(), listener);
                    }
                }
            }

            int accessLevel = share.getAccess();
            switch (accessLevel) {
                case MegaShare.ACCESS_OWNER:
                case MegaShare.ACCESS_FULL: {
                    holder.textViewPermissions.setText(context.getString(R.string.file_properties_shared_folder_full_access));
                    break;
                }
                case MegaShare.ACCESS_READ: {
                    holder.textViewPermissions.setText(context.getString(R.string.file_properties_shared_folder_read_only));
                    break;
                }
                case MegaShare.ACCESS_READWRITE: {
                    holder.textViewPermissions.setText(context.getString(R.string.file_properties_shared_folder_read_write));
                    break;
                }
            }

            if (share.isPending()) {
                String pending = " " + context.getString(R.string.pending_outshare_indicator);
                holder.textViewPermissions.append(pending);
            }
        }

        holder.threeDotsLayout.setTag(holder);
        holder.threeDotsLayout.setOnClickListener(this);
    }

    @Override
    public int getItemCount() {
        return shareList.size();
    }

    public Object getItem(int position) {
        return shareList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public int getPositionClicked() {
        return positionClicked;
    }

    public void setPositionClicked(int p) {
        positionClicked = p;
    }

    @Override
    public void onClick(View v) {
        Timber.d("onClick");
        ViewHolderShareList holder = (ViewHolderShareList) v.getTag();
        int currentPosition = holder.currentPosition;
        final MegaShare s = (MegaShare) getItem(currentPosition);

        int id = v.getId();
        if (id == R.id.shared_folder_three_dots_layout) {
            if (multipleSelect) {
                ((FileContactListActivity) context).itemClick(currentPosition);
            } else {
                ((FileContactListActivity) context).showOptionsPanel(s);
            }
        } else if (id == R.id.shared_folder_item_layout) {
            ((FileContactListActivity) context).itemClick(currentPosition);
        }
    }

    public void setShareList(List<MegaShare> shareList) {
        Timber.d("setShareList");
        this.shareList = shareList;
        positionClicked = -1;
        notifyDataSetChanged();
    }

    public boolean isMultipleSelect() {
        return multipleSelect;
    }

    public void setMultipleSelect(boolean multipleSelect) {
        if (this.multipleSelect != multipleSelect) {
            this.multipleSelect = multipleSelect;
        }
        if (this.multipleSelect) {
            selectedItems = new SparseBooleanArray();
        }
    }

    public void toggleSelection(int pos) {
        if (selectedItems.get(pos, false)) {
            Timber.d("Delete pos: %s", pos);
            selectedItems.delete(pos);
        } else {
            Timber.d("PUT pos: %s", pos);
            selectedItems.put(pos, true);
        }
        notifyItemChanged(pos);

        MegaSharedFolderAdapter.ViewHolderShareList view = (MegaSharedFolderAdapter.ViewHolderShareList) listFragment.findViewHolderForLayoutPosition(pos);
        if (view != null) {
            Timber.d("Start animation: %s", pos);
            Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
            flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (selectedItems.size() <= 0) {
                        ((FileContactListActivity) context).hideMultipleSelect();
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            view.imageView.startAnimation(flipAnimation);
        }
    }

    public void toggleAllSelection(int pos) {
        final int positionToflip = pos;

        if (selectedItems.get(pos, false)) {
            Timber.d("Delete pos: %s", pos);
            selectedItems.delete(pos);
        } else {
            Timber.d("PUT pos: %s", pos);
            selectedItems.put(pos, true);
        }

        Timber.d("Adapter type is LIST");
        MegaSharedFolderAdapter.ViewHolderShareList view = (MegaSharedFolderAdapter.ViewHolderShareList) listFragment.findViewHolderForLayoutPosition(pos);
        if (view != null) {
            Timber.d("Start animation: %s", pos);
            Animation flipAnimation = AnimationUtils.loadAnimation(context, R.anim.multiselect_flip);
            flipAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (selectedItems.size() <= 0) {
                        ((FileContactListActivity) context).hideMultipleSelect();
                    }
                    notifyItemChanged(positionToflip);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            view.imageView.startAnimation(flipAnimation);
        } else {
            Timber.w("NULL view pos: %s", positionToflip);
            notifyItemChanged(pos);
        }
    }

    public void selectAll() {
        for (int i = 0; i < this.getItemCount(); i++) {
            if (!isItemChecked(i)) {
                toggleAllSelection(i);
            }
        }
    }

    public void clearSelections() {
        Timber.d("clearSelections");
        for (int i = 0; i < this.getItemCount(); i++) {
            if (isItemChecked(i)) {
                toggleAllSelection(i);
            }
        }
    }

    public int getSelectedItemCount() {
        return selectedItems.size();
    }

    public List<Integer> getSelectedItems() {
        List<Integer> items = new ArrayList<Integer>(selectedItems.size());
        for (int i = 0; i < selectedItems.size(); i++) {
            items.add(selectedItems.keyAt(i));
        }
        return items;
    }

    public MegaShare getContactAt(int position) {
        try {
            if (shareList != null) {
                return shareList.get(position);
            }
        } catch (IndexOutOfBoundsException e) {
        }
        return null;
    }

    private boolean isItemChecked(int position) {
        return selectedItems.get(position);
    }

    /*
     * Get list of all selected contacts
     */
//	public List<MegaUser> getSelectedUsers() {
//		ArrayList<MegaUser> users = new ArrayList<MegaUser>();
//		
//		for (int i = 0; i < selectedItems.size(); i++) {
//			if (selectedItems.valueAt(i) == true) {
//				MegaUser u = getContactAt(selectedItems.keyAt(i));
//				if (u != null){
//					users.add(u);
//				}
//			}
//		}
//		return users;
//	}

    /*
     * Get list of all selected shares
     */
    public ArrayList<MegaShare> getSelectedShares() {
        ArrayList<MegaShare> shares = new ArrayList<MegaShare>();

        for (int i = 0; i < selectedItems.size(); i++) {
            if (selectedItems.valueAt(i) == true) {
                MegaShare s = getContactAt(selectedItems.keyAt(i));
                if (s != null) {
                    shares.add(s);
                }
            }
        }
        return shares;
    }

    public void setNodes(ArrayList<MegaShare> _shareList) {
        this.shareList = _shareList;
        notifyDataSetChanged();
    }

    public RecyclerView getListFragment() {
        return listFragment;
    }

    public void setListFragment(RecyclerView listFragment) {
        this.listFragment = listFragment;
    }
}
