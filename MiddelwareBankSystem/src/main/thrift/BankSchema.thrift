namespace java exchange.service.thrift


enum Currency{
    USD = 0;
    GBP = 1;
    PLN = 2;
    EUR = 3;
    CHF = 4;
}

enum AccountType {
    Standart = 0,
    Premium = 1
}

struct Money{
    1: double amount
    2: Currency currency
}

struct ID{
    1: string id
}

struct Login{
    1: ID id
    2: Password passwd
}

struct Password{
    1: string password
}

struct PersonalData{
    1: string name
    2: string surname
    3: ID id
    4: Money income
}

struct Account{
    1: PersonalData client
    2: Money balance
    3: Password passwd
    4: AccountType type
}
struct RegisterUserReponse{
    1: Password passwd
    2: AccountType type
}

exception ErrorInOperation{
    1: string reason
}

struct LoanRequest{
    1: Login login
    2: Money money
    3: i32 time
}

struct LoanResponse{
    1: required bool accepted,
    2: optional Money nativeBankCurrencyCost,
    3: optional Money foreginCurrencyCost
}

service RegisterUser{
    RegisterUserReponse register(1:PersonalData person,2:Money initInput) throws (1:ErrorInOperation e)
}

service StandardManager {
    Money getBalance(1:Login login) throws (1: ErrorInOperation e),
}

service PremiumManager extends StandardManager {
    LoanResponse takeLoan(1:LoanRequest loanRequest) throws (1: ErrorInOperation e),
}