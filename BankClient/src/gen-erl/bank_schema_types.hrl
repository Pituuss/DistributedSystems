-ifndef(_bank_schema_types_included).
-define(_bank_schema_types_included, yeah).

-define(BANK_SCHEMA_CURRENCY_USD, 0).
-define(BANK_SCHEMA_CURRENCY_GBP, 1).
-define(BANK_SCHEMA_CURRENCY_PLN, 2).
-define(BANK_SCHEMA_CURRENCY_EUR, 3).
-define(BANK_SCHEMA_CURRENCY_CHF, 4).

-define(BANK_SCHEMA_ACCOUNTTYPE_STANDART, 0).
-define(BANK_SCHEMA_ACCOUNTTYPE_PREMIUM, 1).

%% struct 'Money'

-record('Money', {'amount' :: float() | 'undefined',
                  'currency' :: integer() | 'undefined'}).
-type 'Money'() :: #'Money'{}.

%% struct 'ID'

-record('ID', {'id' :: integer() | 'undefined'}).
-type 'ID'() :: #'ID'{}.

%% struct 'Login'

-record('Login', {'id' :: 'ID'() | 'undefined',
                  'passwd' :: 'Password'() | 'undefined'}).
-type 'Login'() :: #'Login'{}.

%% struct 'Password'

-record('Password', {'password' :: string() | binary() | 'undefined'}).
-type 'Password'() :: #'Password'{}.

%% struct 'PersonalData'

-record('PersonalData', {'name' :: string() | binary() | 'undefined',
                         'surname' :: string() | binary() | 'undefined',
                         'id' :: 'ID'() | 'undefined',
                         'income' :: 'Money'() | 'undefined'}).
-type 'PersonalData'() :: #'PersonalData'{}.

%% struct 'Account'

-record('Account', {'client' :: 'PersonalData'() | 'undefined',
                    'balance' :: 'Money'() | 'undefined',
                    'passwd' :: 'Password'() | 'undefined',
                    'accountType' :: integer() | 'undefined'}).
-type 'Account'() :: #'Account'{}.

%% struct 'RegisterUserReponse'

-record('RegisterUserReponse', {'passwd' :: 'Password'() | 'undefined',
                                'accountType' :: integer() | 'undefined'}).
-type 'RegisterUserReponse'() :: #'RegisterUserReponse'{}.

%% struct 'ErrorInOperation'

-record('ErrorInOperation', {'reason' :: string() | binary() | 'undefined'}).
-type 'ErrorInOperation'() :: #'ErrorInOperation'{}.

%% struct 'LoanRequest'

-record('LoanRequest', {'login' :: 'Login'() | 'undefined',
                        'money' :: 'Money'() | 'undefined',
                        'time' :: integer() | 'undefined'}).
-type 'LoanRequest'() :: #'LoanRequest'{}.

%% struct 'LoanResponse'

-record('LoanResponse', {'accepted' :: boolean(),
                         'nativeBankCurrencyCost' :: 'Money'() | 'undefined',
                         'foreginCurrencyCost' :: 'Money'() | 'undefined'}).
-type 'LoanResponse'() :: #'LoanResponse'{}.

-endif.
