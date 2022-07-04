


HUMAN_RELATED_CATEGORIES = [
    'username', 'birthday', 'birthplace', 'age', 'gender', 'race', 'religion',
    'education_info', 'job_info', 'income', 'marriage', 'biometrical_data',
    'user_preference', 'user_social_identifier', 'user_identifier',
    'app_business_data'
]

COMMON_2C_WORDS = ['id', 'ip', 'os', 'ui', 'qq'] # 2-character words related to privacy
SHORT_PRIVACY_WORDS = [
    'dob', 'age', 'job', 'vip', 'pwd', 'loc', 'lon', 'lng', 'lat', 'gps', 'ip', 'pin',
    'fb', 'qq', 'icq', 'sex', 'zip', 'tax'
]

PRIVACY_UNRELATED_TYPES = [
    r'^androidx\..*\.widget\.',
    r'^androidx\.fragment\.',
    r'^androidx\.lifecycle\.',
    r'android\.widget\.',
    r'.+View$',
    r'.+Type$',
    r'^java\.net\.'
] # some privacy unrelated format

BASIC_TYPES = [
    'byte', 'java.lang.Byte',
    'short', 'java.lang.Short',
    'int', 'java.lang.Integer',
    'long', 'java.lang.Long',
    'float', 'java.lang.Float',
    'double', 'java.lang.Double',
    'boolean', 'java.lang.Boolean',
    'java.lang.String', 'java.lang.CharSequence'
]

COMMON_WIDGETS_RELATED_WORDS = [
    'btn', 'button', 'edt', 'edit', 'ic', 'icon', 'iv', 'tv', 'textview',
    'view'
]

MY_PRIVACY_OR_OBJECT_RELATED_WORDS = [
    "setting", "settings", "error", "upload", "util", "utils", "login", "sign",
    "signup", "registration", "register", "registry", "reg", "self", "forget", "forgot",
    "change", "me", "my", "mine", "current", "config", "configs", "edit",
    "editor", "confirm", "confirmation", "confirmations", "validate",
    "validation", "verify", "log", "logs", "version", "reset", "restore",
    "default", "preference", "preferences", "filter", "filters",
    "notification", "notifications", "auth", "authentication",
    "authentications", "faq", "faqs", "policy", "set", "term", "terms",
    "update", "template", "local", "host", "agreement", "google", "facebook",
    "twitter", "server", "socket", "widget", "view", "activity", "fragment",
    "dialog", "toast", "layout", "shop", "market", "store", "grocery"
]

PROFILE_ENTRY_WORDS = [
    'account', 'user', 'profile', 'personalinfo', 'personal', 'friend',
    'partner', 'member', 'users', 'encounter', 'actor', 'viewer', 'author',
    'consignor', 'author', 'consignor', 'player',
    'follower', 'liker', 'owner', 'receiver', 'winner', 'blogger', 'publisher',
    'customer', 'teacher', 'prayer', 'retailer', 'drawer', 'organizer',
    'consumer', 'painter', 'dealer', 'buyer', 'speaker', 'passenger', 'leader',
    'organiser', 'purchaser', 'sharer', 'advertiser', 'writer', 'manufacturer',
    'payer', 'developer', 'driver', 'traveler', 'reorder', 'singer',
    'recorder', 'walker', 'reviewer', 'shopper', 'runner', 'rider',
    'cardholder', 'subscriber', 'insurer', 'trainer', 'coder', 'employer',
    'reporter', 'producer', 'traveller', 'supplier', 'caller', 'lender',
    'renter', 'borrower', 'astrologer', 'orderer', 'commuter', 'stranger',
    'inviter', 'gifter', 'uploader', 'likers', 'fetcher', 'ticketmaster',
    'reseller', 'recommender', 'people', 'advisor', 'parent', 'child',
    'student', 'worker', 'merchant', 'participant', 'sponsor', 'artist',
    'reader', 'neighbor', 'instructor', 'payee', 'patient', 'witness',
    'consultant', 'teammate', 'competitor', 'doctor', 'supporter'
]