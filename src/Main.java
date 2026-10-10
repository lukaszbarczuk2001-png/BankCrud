import bank.ui.ConsoleApp;

public class Main {
    public static void main(String[] args) {
        // Plik bank.db powstanie w folderze projektu przy pierwszym uruchomieniu.
        new ConsoleApp("jdbc:sqlite:bank.db").run();
    }
}
