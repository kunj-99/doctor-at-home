package com.infowave.thedoctorathomeuser;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.infowave.thedoctorathomeuser.adapter.AnimalAdapter;

import java.util.ArrayList;
import java.util.List;

public class VetAnimalsActivity extends AppCompatActivity implements AnimalAdapter.OnAnimalClickListener {

    private RecyclerView recyclerView;
    private AnimalAdapter adapter;
    private final List<Animal> animals = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vet_animals);

        recyclerView = findViewById(R.id.rvAnimals);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.addItemDecoration(new GridSpacingDecoration(16)); // spacing between cards

        seedAnimals(); // add animals
        adapter = new AnimalAdapter(animals, this);
        recyclerView.setAdapter(adapter);
    }

    private void seedAnimals() {
        // Add your drawables in res/drawable and reference here.
        // Replace sample names with your actual images.
        animals.add(new Animal("Dog", R.drawable.animal_dog));
        animals.add(new Animal("Cat", R.drawable.animal_cat));
        animals.add(new Animal("Cow", R.drawable.animal_cow));
        animals.add(new Animal("Buffalo", R.drawable.animal_buffalo));
        animals.add(new Animal("Goat", R.drawable.animal_goat));
//        animals.add(new Animal("Sheep", R.drawable.animal_sheep));
//        animals.add(new Animal("Horse", R.drawable.animal_horse));
//        animals.add(new Animal("Camel", R.drawable.animal_camel));
//        animals.add(new Animal("Rabbit", R.drawable.animal_rabbit));
//        animals.add(new Animal("Parrot", R.drawable.animal_parrot));
        // Add more as you need
    }

    @Override
    public void onAnimalClick(Animal animal) {
        // Navigate to your vet appointment flow with selected animal
        // Example:
        Intent i = new Intent(this, VetAppointmentActivity.class);
        i.putExtra("animal_name", animal.getName());
        startActivity(i);
    }
}
