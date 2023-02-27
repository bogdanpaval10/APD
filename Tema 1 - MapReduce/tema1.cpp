#include <iostream>
#include <stdlib.h>
#include <pthread.h>
#include <fstream>
#include <cstring>
#include <algorithm>
#include <vector>
#include <cmath>
using namespace std;


struct my_arg_map {
	int nr_maps;
	int nr_reduces;
    int thread_id;
    vector<string> *in_files; // namele fisierelor de intrare
    pthread_mutex_t *mutex;
    pthread_barrier_t *barrier;
    vector<vector<vector<long>>> *all_lists; // liste partiale pt fiecare putere
};


struct my_arg_reducer {
	int nr_maps;
	int nr_reduces;
    int thread_id;
    pthread_barrier_t *barrier;
    vector<vector<vector<long>>> *all_lists; // liste partiale pt fiecare putere
};


bool cmp(string file1, string file2)
{
    FILE *fin1 = fopen(file1.c_str(), "r");
    
    fseek(fin1, 0, SEEK_END); // muta cursorul la finalul fisierului
    long size1 = ftell(fin1); // afla dimensiunea fisierului
    fclose(fin1);

    FILE *fin2 = fopen(file2.c_str(), "r");
    fseek(fin2, 0, SEEK_END); // muta cursorul la finalul fisierului
    long size2 = ftell(fin2); // afla dimensiunea fisierului
    fclose(fin2);
    
    return (size1 < size2); // sorteaza crescator fisierele
}


int test_power(long n, vector<int> *powers) // intoarce puterile prin prin vectorul powers
{
    int exponent = 2;
    long low, high;

    while (1) {
        if (pow(2, exponent) > n) {
            break;
        }

        low = 2;
        high = low;
        while (pow(high, exponent) <= n) {
            high *= 2;
        }
        while (high - low > 1) {
            long middle = (low + high) / 2;
            if (pow(middle, exponent) <= n) {
                low = middle;
            } else {
                high = middle;
            }
        }
        
        if (pow(low, exponent) == n) {
            powers->push_back(exponent); // adaug in vectorul de puteri, exponentul curent
            if (test_power(low, powers) == 1) { // daca baza este la randul ei putere perfecta
                test_power(low, powers); // apeleaza functia si pt aceasta valoare
            } else {
                return 1; // s-au terminat de gasit puterile perfecte
            }
        }
        exponent++;
    }
    return 0;
}


void map_helper(ifstream &fin, struct my_arg_map *data)
{
    int nr_elements;
    long curr_element;
    fin >> nr_elements;

    while (fin >> curr_element) { // citesc namerele din fisier
        if (curr_element == 1) { // daca nr este 1, apare in toate listele partiale
            for (int i = 0; i < data->nr_reduces; i++) {
                (*data->all_lists)[data->thread_id][i].push_back(curr_element);
            }
        } 
        if (curr_element > 1) {
            vector<int> powers; // vector cu puterile perfecte ale lui curr_element
            test_power(curr_element, &powers); // functia care completeaza vectorul powers

            std::sort(powers.begin(), powers.end()); // sorteaza vectorul in ordine crescatoare
            powers.erase(std::unique(powers.begin(), powers.end()),powers.end()); // elimina duplicatele

            for (int i = 0; i < (int)powers.size(); i++) {
                if (powers[i] <= data->nr_reduces + 1) { // puterea gasita sa fie maxim R + 1
                    // adaugam nr in lista partiala a puterii powers[i] (la mine powers[i] - 2, am inceput de la 0)
                    (*data->all_lists)[data->thread_id][powers[i] - 2].push_back(curr_element);
                }
            }
        }
    }
}


void *f_maps(void *arg)
{
    struct my_arg_map *data = (struct my_arg_map*)arg;
    char name[25];
    
    while (!((*data->in_files).empty())) { // mai sunt fisiere in vector ce nu au fost citite
        pthread_mutex_lock(data->mutex);
        strcpy(name, ((*data->in_files).back()).c_str()); // namele fisierului din care se va face citirea
        (*data->in_files).pop_back(); // scot din vector fisierul citit
        pthread_mutex_unlock(data->mutex);
        
        ifstream fin;
        fin.open(name);
        map_helper(fin, data); // se apeleaza functia care rezolva operatiile cerute

        fin.close();
    }
    pthread_barrier_wait(data->barrier);

	pthread_exit(NULL);
}

void reducer_helper(ofstream &fout, struct my_arg_reducer *data)
{
    vector<int> combine; // vector in care se combina toate nr din listele partiale de putere thread_id + 2
    for (int i = 0; i < data->nr_maps; i++) {
        for (int j = 0; j < (int)(*data->all_lists)[i][data->thread_id].size(); j++) {
            combine.push_back((*data->all_lists)[i][data->thread_id][j]); // fiecare nr din M[i][thread_id]
        }
    }
    std::sort(combine.begin(), combine.end()); // sorteaza vectorul in ordine crescatoare
    // elimina duplicatele pt a putea numara elementele unice
    combine.erase(std::unique(combine.begin(), combine.end()), combine.end());
    
    fout << combine.size(); // afiseaza numarul de valori unice din lista agregata
}

void *f_reducers(void *arg)
{
    struct my_arg_reducer *data = (struct my_arg_reducer*)arg;

    pthread_barrier_wait(data->barrier);
    
    char name[25];
    strcpy(name, "out");
    strcat(name, to_string(data->thread_id + 2).c_str());
    strcat(name, ".txt");

    ofstream fout;
    fout.open(name);
    reducer_helper(fout, data); // functia care combina rezultatele maperilor
    fout.close();

	pthread_exit(NULL);
}


int main(int argc, char **argv)
{
	int i, j, r_map, r_reducer, nr_maps, nr_reduces, nr_files, max_threads;
	void *status_map, *status_reducer;
    vector<vector<vector<long>>> all_lists;
    vector<string> in_files;
    string name;
    ifstream fin;

    nr_maps = atoi(argv[1]);
    nr_reduces = atoi(argv[2]);
    max_threads = nr_maps + nr_reduces;
    
    fin.open(argv[3]);
    fin >> nr_files;
    getline(fin, name); // trebuie citit "\n"-ul
    
    for (i = 0; i < nr_files; i++) {
        getline(fin, name); // citeste fiecare fisier
        in_files.push_back(name);
    }
    sort(in_files.begin(), in_files.end(), cmp); // sorteaza fisierele crescator dupa dimensiune

    fin.close();

	pthread_t mappers[nr_maps];
    pthread_t reducers[nr_reduces];
    
    struct my_arg_map arguments_map[nr_maps];
    struct my_arg_reducer arguments_reducer[nr_reduces];

    pthread_barrier_t barrier;
    pthread_mutex_t mutex;

	pthread_barrier_init(&barrier, NULL, max_threads);
    pthread_mutex_init(&mutex, NULL);

    for (i = 0; i < nr_maps; i++) { // se initializeaza vectorii de liste si listele partiale
        vector<vector<long>> vector_M_i;
        for (j = 0; j < nr_reduces; j++) {
            vector<long> vector_R_i;
            vector_M_i.push_back(vector_R_i);
        }
        all_lists.push_back(vector_M_i);
    }

    for (i = 0; i < nr_maps; i++) {
        arguments_map[i].nr_maps = nr_maps;
        arguments_map[i].nr_reduces = nr_reduces;
        arguments_map[i].thread_id = i;
        arguments_map[i].barrier = &barrier;
        arguments_map[i].mutex = &mutex;
        arguments_map[i].in_files = &in_files;
        arguments_map[i].all_lists = &all_lists; 
    }
    for (i = 0; i < nr_reduces; i++) {
        arguments_reducer[i].nr_maps = nr_maps;
        arguments_reducer[i].nr_reduces = nr_reduces;
        arguments_reducer[i].thread_id = i;
        arguments_reducer[i].barrier = &barrier;
        arguments_reducer[i].all_lists = &all_lists;
	}

	for (i = 0; i < max_threads; i++) {
        if (i < nr_maps) {    
		    r_map = pthread_create(&mappers[i], NULL, f_maps, &arguments_map[i]);
        }
        if (i < nr_reduces) {
		    r_reducer = pthread_create(&reducers[i], NULL, f_reducers, &arguments_reducer[i]);
        }

        if (r_map || r_reducer) {
			cout << "Eroare la crearea thread-ului " << i << endl;
			exit(-1);
		}
	}

	for (i = 0; i < max_threads; i++) {
        if (i < nr_maps) {
            r_map = pthread_join(mappers[i], &status_map);
        }
		if (i < nr_reduces) {
            r_reducer = pthread_join(reducers[i], &status_reducer);
        }
        
		if (r_map || r_reducer) {
			cout << "Eroare la thread-ul " << i << endl;
			exit(-1);
		}
	}

    pthread_mutex_destroy(&mutex);
	pthread_barrier_destroy(&barrier);

	return 0;
}
