package FPS.Watcher;

import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

public class BinderContainer implements Parcelable { // implements Parcelable 之后，这个类就可以作为intent的附加参数了。
    public final IBinder binder;

    public BinderContainer(IBinder binder) {
        this.binder = binder;
    }


    protected BinderContainer(Parcel in) {
        binder = in.readStrongBinder();
    }

    public static final Creator<BinderContainer> CREATOR = new Creator<>() {
        @Override
        public BinderContainer createFromParcel(Parcel in) {
            return new BinderContainer(in);
        }

        @Override
        public BinderContainer[] newArray(int size) {
            return new BinderContainer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStrongBinder(binder);
    }
}