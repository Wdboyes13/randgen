#include <stdio.h>
#include <stdlib.h>
#include <time.h>

int main(){
    srand(time(NULL));
    int min;
    int max;
    printf("Enter Minimum: ");
    scanf("%d", &min);
    printf("Enter Maximum: ");
    scanf("%d", &max);
    int random_number = (rand() % (max-min+1) ) + min;
    printf("%d\n", random_number);
    return 0;
}