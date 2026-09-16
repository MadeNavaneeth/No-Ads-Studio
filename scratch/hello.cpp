#include <iostream>
#include <string_view>

int main(int argc, char **argv) {
    std::string_view who = argc > 1 ? argv[1] : "world";
    std::cout << "hello from C++, " << who << '\n';
    return 0;
}
