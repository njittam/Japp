package nl.rsdt.japp.jotial.maps.pinning;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Marker;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

import nl.rsdt.japp.jotial.Recreatable;
import nl.rsdt.japp.jotial.io.AppData;
import nl.rsdt.japp.jotial.maps.management.MarkerIdentifier;

/**
 * @author Dingenis Sieger Sinke
 * @version 1.0
 * @since 8-9-2016
 * Description...
 */
public class PinningManager implements OnMapReadyCallback, Recreatable, GoogleMap.OnInfoWindowLongClickListener {

    protected static final String STORAGE_ID = "PinData";

    protected static final String BUNDLE_KEY = "PinningManager";

    protected Context context;

    protected GoogleMap googleMap;

    protected ArrayList<Pin> pins = new ArrayList<>();

    protected ArrayList<Pin.Data> buffer = new ArrayList<>();

    public void intialize(Context context) {
        this.context = context;
    }

    public void onCreate(Bundle savedInstanceState)
    {
        ArrayList<Pin.Data> pins;
        if(savedInstanceState != null) {
            pins = savedInstanceState.getParcelableArrayList(BUNDLE_KEY);
        } else {
            pins = AppData.getObject(STORAGE_ID, new TypeToken<ArrayList<Pin.Data>>(){}.getType());
        }

        if(pins != null && !pins.isEmpty()) {
            if(googleMap != null) {
                process(pins);
            } else {
                buffer = pins;
            }
        }
    }

    public void onSaveInstanceState(Bundle state)
    {
        ArrayList<Pin.Data> list = new ArrayList<>();
        Pin current;
        for(int i = 0; i < pins.size(); i++) {
            current = pins.get(i);

            if(current != null) {
                list.add(current.data);
            }

        }
        state.putParcelableArrayList(BUNDLE_KEY, list);
    }

    public void add(Pin pin) {
        pins.add(pin);
        save(true);
    }

    public void remove(Pin pin) {
        pins.remove(pin);
        save(true);
    }

    @Nullable
    public Pin findAssocaited(Marker marker) {
        Pin current;
        for(int i = 0; i < pins.size(); i++) {
            current = pins.get(i);
            if(current != null) {
                if(current.marker.getId().equals(marker.getId())) {
                    return current;
                }
            }
        }
        return null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        googleMap.setOnInfoWindowLongClickListener(this);

        if(buffer != null) {
            process(buffer);
            buffer.clear();
            buffer = null;
        }
    }

    private void process(ArrayList<Pin.Data> input) {
        if(googleMap != null) {
            Pin.Data buffer;
            for(int i = 0; i < input.size(); i++) {
                buffer = input.get(i);

                if(buffer != null) {
                    pins.add(Pin.create(googleMap, buffer));
                }
            }
        }
    }

    private void save(boolean background) {
        /**
         * Save the pins
         * */
        ArrayList<Pin.Data> list = new ArrayList<>();
        Pin current;
        for(int i = 0; i < pins.size(); i++) {
            current = pins.get(i);

            if(current != null) {
                list.add(current.data);
            }
        }
        if(background) {
            AppData.saveObjectAsJsonInBackground(list, STORAGE_ID);
        } else {
            AppData.saveObjectAsJson(list, STORAGE_ID);
        }

    }

    public void onDestroy() {
        save(false);

        if(googleMap != null) {
            googleMap = null;
        }

        if(pins != null){
            pins.clear();
            pins = null;
        }

        if(buffer != null) {
            buffer.clear();
            buffer = null;
        }

    }

    @Override
    public void onInfoWindowLongClick(final Marker marker) {
        MarkerIdentifier identifier = new Gson().fromJson(marker.getTitle(), MarkerIdentifier.class);
        if(identifier != null) {
            switch (identifier.getType()) {
                case MarkerIdentifier.TYPE_PIN:
                    new AlertDialog.Builder(context)
                            .setTitle("Verwijderen van Mark")
                            .setMessage("Weet je zeker dat je deze Mark wilt verwijderen?")
                            .setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Pin assoicated = findAssocaited(marker);
                                    if(assoicated != null) {
                                        pins.remove(assoicated);
                                    }
                                    marker.remove();
                                    save(true);
                                }
                            })
                            .setNegativeButton("Nee", null)
                            .create()
                            .show();
                    break;
            }
        }
    }

}
